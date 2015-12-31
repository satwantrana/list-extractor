package edu.iitd.nlp.ListExtraction

import org.allenai.common.LoggingWithUncaughtExceptions
import org.allenai.nlpstack.core.{ ChunkedToken, PostaggedToken }

import scala.collection.mutable
import scala.io.Source

object FineToCoarsePostags {
  val FineToCoarsePostagMap = Source.fromFile("data/fine-coarse-postags").getLines()
    .map {
      case l =>
        val s = l.split("\t")
        (s(0), s(1))
    }.toMap

  def convert(fine: String) = FineToCoarsePostagMap.getOrElse(fine, "OTHER")
}

class WordVectorWrapper extends LoggingWithUncaughtExceptions {
  val modelFile = "models/open_ie_embeddings.words"
  val wordVectors = mutable.Map[String, Seq[Double]]()
  var numWords = 0
  var dim = 300
  var DEBUG = false
  readModel()

  def readModel(): Unit = {
    logger.info("Loading Word Vectors")
    val lines = Source.fromFile(modelFile).getLines()
    val firstLine = lines.next().split(" ").map(_.toInt)
    numWords = firstLine(0)
    dim = firstLine(1)
    for (i <- 0 until numWords) {
      val line = lines.next().split(" ")
      wordVectors(line.head) = line.tail.map(_.toDouble)
      val norm = getVectorNorm(wordVectors(line.head))
      wordVectors(line.head) = wordVectors(line.head).map(_ / norm)
    }
    logger.info("Loaded Word Vectors")
  }

  def getVectorNorm(a: Seq[Double]): Double = {
    Math.sqrt(a.map(d => d * d).sum)
  }

  def getUnitVector(a: Seq[Double]): Seq[Double] = {
    val norm = getVectorNorm(a)
    a.map(_ / norm)
  }

  def getVectorDotProduct(u: Seq[Double], v: Seq[Double]): Double = {
    u.zip(v).map { case (d1, d2) => d1 * d2 }.sum
  }

  val wordSimilarityCache = mutable.Map[(String, String), Double]()
  def getWordSimilarity(a: String, b: String): Double = {
    if (a == b) 1
    else if (!wordVectors.contains(a) || !wordVectors.contains(b)) 0
    else wordSimilarityCache.synchronized {
      if (!wordSimilarityCache.contains((a, b))) wordSimilarityCache((a, b)) = (getVectorDotProduct(wordVectors(a), wordVectors(b)) + 1.0) / 2.0
      wordSimilarityCache((a, b))
    }
  }

  def getBagOfWordsPhraseSimilarity(a: Seq[String], b: Seq[String]): Double = {
    def getSumOfVectors(M: Seq[Seq[Double]]): Seq[Double] = {
      if (M.isEmpty) Seq.fill(dim)(0)
      else M.reduceLeft((U, V) => U.zip(V).map { case (d1, d2) => d1 + d2 })
    }

    val U = getUnitVector(getSumOfVectors(a.filter(wordVectors.contains).map(wordVectors)))
    val V = getUnitVector(getSumOfVectors(b.filter(wordVectors.contains).map(wordVectors)))
    getVectorDotProduct(U, V)
  }

  def getDPPhraseSimilarity(a: Seq[String], b: Seq[String]): Double = {
    case class Entry(var value: Double = Double.NegativeInfinity, var num: Double = 0, var prev: (Int, Int) = (-1, -1)) {
      //Negative infinity to avoid matching to an empty set
      def update(other: Entry, sim: Double, idx: Int, jdx: Int, incrNum: Int = 1) {
        require(sim >= 0 && sim <= incrNum, s"$sim $incrNum")
        if (num == 0 || value / num < (other.value + sim) / (other.num + incrNum)) {
          value = other.value + sim
          num = other.num + incrNum
          prev = (idx, jdx)
        }
      }
    }

    val (n, m) = (a.size, b.size)
    val dp = mutable.ArrayBuffer.fill(n + 1)(mutable.ArrayBuffer.fill(m + 1)(new Entry()))
    dp(0)(0) = new Entry(value = 0, num = 0)
    val simWindow = 3
    for (i <- 0 until n; j <- 0 until m) {
      var sim: Double = 0
      for (k <- 0 to Math.min(j, simWindow)) {
        sim += getWordSimilarity(a(i), b(j - k))
        dp(i + 1)(j + 1).update(dp(i)(j - k), sim, i, j - k, k + 1)
      }
      sim = 0
      for (k <- 0 to Math.min(i, simWindow)) {
        sim += getWordSimilarity(a(i - k), b(j))
        dp(i + 1)(j + 1).update(dp(i - k)(j), sim, i - k, j, k + 1)
      }
    }

    if (DEBUG) {
      var (tx, ty) = (n, m)
      val matches = mutable.ArrayBuffer[(String, String)]()
      while (tx != -1 || ty != -1) {
        matches += ((a(tx), b(ty)))
        val (px, py) = dp(tx)(ty).prev
        tx = px
        ty = py
      }
      logger.info(s"Matches $dp $matches")
    }
    if (dp(n)(m).value == Double.NegativeInfinity) 0
    else if (dp(n)(m).num == 0) dp(n)(m).value
    else dp(n)(m).value / dp(n)(m).num
  }

  def sigmoid(inp: Double) = inp // 1.0 / (1.0 + Math.exp(-inp))

  val numFeatures = FeatureVector.defaultNumFeatures
  def getFeatureDPPhraseSimilarity(a: Seq[ChunkedToken], b: Seq[ChunkedToken],
    wv: FeatureVector = FeatureVector.Default(numFeatures), params: Params): FeatureVector = {
    case class Entry(
        var value: FeatureVector = FeatureVector.Zeros(numFeatures),
        var num: Double = 0, var prev: (Int, Int) = (-1, -1)
    ) {
      def update(other: Entry, sim: FeatureVector, idx: Int, jdx: Int, incrNum: Int = 1) {
        if (num == 0 || sigmoid(value * wv.normalised / num) < sigmoid((other.value + sim) * wv.normalised / (other.num + incrNum))) {
          value = other.value + sim
          num = other.num + incrNum
          prev = (idx, jdx)
        }
      }
    }
    def getWordSimVector(u: ChunkedToken, v: ChunkedToken): FeatureVector = {
      val sim = getWordSimilarity(u.string, v.string)
      val samePOS = if (FineToCoarsePostags.convert(u.postag) == FineToCoarsePostags.convert(v.postag)) 1.0 else 0.0
      val sameChunk = if (u.chunk.drop(2) == v.chunk.drop(2)) 1.0 else 0.0
      FeatureVector(mutable.ArrayBuffer(1.0, sim, samePOS, sameChunk, Math.abs(params.leftDis), Math.abs(params.rightDis)))
    }
    val (n, m) = (a.size, b.size)
    val dp = mutable.ArrayBuffer.fill(n + 1)(mutable.ArrayBuffer.fill(m + 1)(new Entry()))
    dp(0)(0) = new Entry(value = FeatureVector.Zeros(numFeatures), num = 0)

    val simWindow = 3
    for (i <- 0 until n; j <- 0 until m) {
      var sim = FeatureVector.Zeros(numFeatures)
      for (k <- 0 to Math.min(j, simWindow)) {
        sim += getWordSimVector(a(i), b(j - k))
        dp(i + 1)(j + 1).update(dp(i)(j - k), sim, i, j - k, k + 1)
      }
      sim = FeatureVector.Zeros(numFeatures)
      for (k <- 0 to Math.min(i, simWindow)) {
        sim += getWordSimVector(a(i - k), b(j))
        dp(i + 1)(j + 1).update(dp(i - k)(j), sim, i - k, j, k + 1)
      }
    }

    if (DEBUG) {
      var (tx, ty) = (n, m)
      val matches = mutable.ArrayBuffer[(ChunkedToken, ChunkedToken)]()
      while (tx > 0 && ty > 0) {
        matches += ((a(tx - 1), b(ty - 1)))
        val (px, py) = dp(tx)(ty).prev
        tx = px
        ty = py
      }
      logger.info(s"Matches: $matches\nDP: $dp'")
    }
    if (dp(n)(m).value == FeatureVector.NegativeInfinities(numFeatures)) FeatureVector.Zeros(numFeatures)
    else if (dp(n)(m).num == 0) dp(n)(m).value
    else dp(n)(m).value / dp(n)(m).num
  }
}

object WordVectorWrapperMain extends App with LoggingWithUncaughtExceptions {
  val wordVectorWrapper = new WordVectorWrapper
  val bagOfWordsSimilarity = wordVectorWrapper.getBagOfWordsPhraseSimilarity(
    Seq("playing", "cricket"),
    Seq("watching", "television")
  )
  val dpSimilarity = wordVectorWrapper.getDPPhraseSimilarity(
    Seq("playing", "cricket"),
    Seq("watching", "television")
  )
  logger.info(s"Similarity: $bagOfWordsSimilarity $dpSimilarity")
}
