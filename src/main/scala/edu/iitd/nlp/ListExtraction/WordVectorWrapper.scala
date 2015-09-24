package edu.iitd.nlp.ListExtraction

import org.allenai.common.LoggingWithUncaughtExceptions

import scala.collection.mutable
import scala.io.Source

class WordVectorWrapper extends LoggingWithUncaughtExceptions {
  val modelFile = "models/open_ie_embeddings.words"
  val wordVectors = mutable.Map[String, Seq[Double]]()
  readModel()
  def readModel(): Unit = {
    val lines = Source.fromFile(modelFile).getLines()
    val firstLine = lines.next().split(" ").map(_.toInt)
    val (numWords, dim) = (firstLine(0), firstLine(1))
    for (i <- 0 until numWords) {
      val line = lines.next().split(" ")
      wordVectors(line.head) = line.tail.map(_.toDouble)
    }
    logger.info("Loaded Word Vectors")
  }
  def getVectorDotProduct(U: Seq[Double], V: Seq[Double]): Double = {
    U.zip(V).map { case (d1, d2) => d1 * d2 }.sum
  }
  def getWordSimilarity(a: String, b: String): Double = {
    if (a == b) 1
    else if (!wordVectors.contains(a) || !wordVectors.contains(b)) 0
    else getVectorDotProduct(wordVectors(a), wordVectors(b))
  }
  def getBagOfWordsPhraseSimilarity(a: Seq[String], b: Seq[String]): Double = {
    def getSumOfVectors(M: Seq[Seq[Double]]): Seq[Double] = {
      M.reduceLeft((U, V) => U.zip(V).map { case (d1, d2) => d1 + d2 })
    }

    def getUnitVector(a: Seq[Double]): Seq[Double] = {
      val norm = a.map(d => d * d).sum
      a.map(_ / norm)
    }

    val U = getUnitVector(getSumOfVectors(a.filter(wordVectors.contains).map(wordVectors)))
    val V = getUnitVector(getSumOfVectors(b.filter(wordVectors.contains).map(wordVectors)))
    getVectorDotProduct(U, V)
  }
}

object WordVectorWrapperMain extends App with LoggingWithUncaughtExceptions {
  val wordVectorWrapper = new WordVectorWrapper
  val similarity = wordVectorWrapper.getBagOfWordsPhraseSimilarity(
    Seq("playing", "cricket"),
    Seq("watching", "television")
  )
  logger.info(s"Similarity: $similarity")
}
