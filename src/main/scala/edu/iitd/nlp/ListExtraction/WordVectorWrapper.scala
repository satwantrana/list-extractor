package edu.iitd.nlp.ListExtraction

import org.allenai.common.LoggingWithUncaughtExceptions

import scala.collection.mutable
import scala.io.Source

class WordVectorWrapper extends LoggingWithUncaughtExceptions {
  val modelFile = "models/open_ie_embeddings.words"
  val wordVectors = mutable.Map[String, Seq[Double]]()
  var numWords = 0
  var dim = 300
  val DEBUG = false
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
      wordVectors(line.head) = wordVectors(line.head).map(_/norm)
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

  def getWordSimilarity(a: String, b: String): Double = {
    if (a == b) 1
    else if (!wordVectors.contains(a) || !wordVectors.contains(b)) 0
    else getVectorDotProduct(wordVectors(a), wordVectors(b))
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
      def update(other: Entry, sim: Double, idx: Int, jdx: Int) {
        if (value / num < (other.value + sim) / (other.num + 1)) {
          value = other.value + sim
          num = other.num + 1
          prev = (idx, jdx)
        }
      }
    }

    val (n, m) = (a.size, b.size)
    val dp = mutable.ArrayBuffer.fill(n + 1)(mutable.ArrayBuffer.fill(m + 1)(new Entry()))
    dp(0)(0) = new Entry(value = 0, num = 0)

    for (i <- 0 until n; j <- 0 until m) {
      dp(i + 1)(j + 1).update(dp(i)(j + 1), getWordSimilarity(a(i), b(j)), i, j + 1)
      dp(i + 1)(j + 1).update(dp(i + 1)(j), getWordSimilarity(a(i), b(j)), i + 1, j)
      dp(i + 1)(j + 1).update(dp(i)(j), getWordSimilarity(a(i), b(j)), i, j)
    }

    if (DEBUG) {
      var (tx, ty) = (n, m)
      val matches = mutable.ArrayBuffer[(String, String)]()
      while (tx != -1 || ty != -1) {
        val (px, py) = dp(tx)(ty).prev
        if (px == tx - 1 && py == ty - 1 && tx > 0 && ty > 0) matches += ((a(tx - 1), b(ty - 1)))
        tx = px
        ty = py
      }
      logger.info(s"Matches $dp $matches")
    }
    dp(n)(m).value / dp(n)(m).num
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
