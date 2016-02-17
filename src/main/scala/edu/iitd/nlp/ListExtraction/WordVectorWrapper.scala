package edu.iitd.nlp.ListExtraction

import java.util.concurrent.atomic.AtomicBoolean

import org.allenai.common.LoggingWithUncaughtExceptions
import org.allenai.nlpstack.core.{ ChunkedToken, PostaggedToken }
import tokenize.{ defaultTokenizer => tokenizer }

import scala.collection.mutable
import scala.io.Source
import scala.collection.JavaConverters

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
  val openIEVectorsFile = "models/open_ie_embeddings.words"
  val depVectorsFile = "models/deps.words"
  val bagOfWordsVectorsFile = "models/bow5.words"
  val googleNewsVectorsFile = "models/GoogleNews-vectors-negative300.bin"
  val freebaseVectorsFile = "models/knowledge-vectors-skipgram1000.bin"
  val modelFile = openIEVectorsFile
  var sentences: Seq[String] = Seq()
  val wordVectors = mutable.Map[String, Seq[Double]]()
  val initialised = new AtomicBoolean(false)
  var numWords = 0
  var dim = 300
  var DEBUG = false

  def readModel(): Unit = {
    logger.info(s"Creating word filter from ${sentences.size} sentences")
    val wordsToBeLoaded = sentences.flatMap(s => tokenizer.tokenize(s).map(_.string.toLowerCase)).toSet
    logger.info("Loading Word Vectors")
    val lines = Source.fromFile(modelFile).getLines()
    if(modelFile.endsWith(".bin")) {
      val word2vec = new Word2Vec
      word2vec.load(modelFile)
      numWords = word2vec.numWords
      dim = word2vec.vecSize
      wordVectors.clear()
      word2vec.vocab.toMap.foreach{
        case (s,v) => if(wordsToBeLoaded.contains(s)){
          require(s == s.toLowerCase)
          wordVectors(s) = v.toSeq.map(_.toDouble)
        }
      }
    } else if(modelFile == openIEVectorsFile) {
      val firstLine = lines.next().split(" ").map(_.toInt)
      numWords = firstLine(0)
      dim = firstLine(1)
      for (i <- 0 until numWords) {
        val line = lines.next().split(" ")
        if (wordsToBeLoaded.contains(line.head)) {
          wordVectors(line.head) = line.tail.map(_.toDouble)
          val norm = getVectorNorm(wordVectors(line.head))
          wordVectors(line.head) = wordVectors(line.head).map(_ / norm)
        }
      }
    } else {
      while(lines.hasNext) {
        numWords += 1
        val line = lines.next().split(" ")
        if (wordsToBeLoaded.contains(line.head)) {
          wordVectors(line.head) = line.tail.map(_.toDouble)
          dim = wordVectors(line.head).size
          val norm = getVectorNorm(wordVectors(line.head))
          wordVectors(line.head) = wordVectors(line.head).map(_ / norm)
        }
      }
    }
    initialised.set(true)
    logger.info(s"Created word filter with ${wordVectors.size} out of ${wordsToBeLoaded.size} words: ${wordsToBeLoaded.slice(0,4)}")
    logger.info("Loaded Word Vectors")
  }

  def getVectorNorm(a: Seq[Double]): Double = {
    Math.sqrt(a.map(d => d * d).sum)
  }

  def getUnitVector(a: Seq[Double]): Seq[Double] = {
    val norm = getVectorNorm(a)
    if(norm == 0) a
    else a.map(_ / norm)
  }

  def getVectorDotProduct(u: Seq[Double], v: Seq[Double]): Double = {
    if(u.isEmpty || v.isEmpty) 0
    else u.zip(v).map { case (d1, d2) => d1 * d2 }.sum
  }

  val wordSimilarityCache = mutable.Map[(String, String), Double]()
  def getWordSimilarity(_a: String, _b: String): Double = {
    initialised.synchronized {
      if (!initialised.get()) readModel()
    }
    val a = _a.toLowerCase
    val b = _b.toLowerCase
    if (a == b) 1
    else if (!wordVectors.contains(a) || !wordVectors.contains(b)) 0
    else wordSimilarityCache.synchronized {
      if (!wordSimilarityCache.contains((a, b))) wordSimilarityCache((a, b)) = (getVectorDotProduct(wordVectors(a), wordVectors(b)) + 1.0) / 2.0
      wordSimilarityCache((a, b))
    }
  }

  def getWordVector(a: String): Seq[Double] = {
    initialised.synchronized {
      if (!initialised.get()) readModel()
    }
    if (!wordVectors.contains(a)) Seq.fill(dim)(0)
    else wordVectors(a)
  }

  def getBagOfWordsPhraseSimilarity(a: Seq[String], b: Seq[String]): Double = {
    def getSumOfVectors(M: Seq[Seq[Double]]): Seq[Double] = {
      if (M.isEmpty) Seq.fill(dim)(0)
      else M.reduceLeft((U, V) => U.zip(V).map { case (d1, d2) => d1 + d2 })
    }
    val AS = getSumOfVectors(a.map(getWordVector))
    val BS = getSumOfVectors(b.map(getWordVector))
    val U = getUnitVector(AS)
    val V = getUnitVector(BS)
    val res = getVectorDotProduct(U, V)
    if(res.isNaN){
      logger.info(s"Similarity of $a and $b is $res")
    }
    res
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
      FeatureVector(mutable.ArrayBuffer(0.0, sim, samePOS, sameChunk,Math.abs(params.leftDis), Math.abs(params.rightDis)))
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
    else{
      dp(n)(m).value.vec(0) = getBagOfWordsPhraseSimilarity(a.map(_.string), b.map(_.string))
      dp(n)(m).value / dp(n)(m).num
    }
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
