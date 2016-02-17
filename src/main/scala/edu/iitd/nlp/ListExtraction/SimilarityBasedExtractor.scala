package edu.iitd.nlp.ListExtraction

import java.io.{ File, PrintWriter }
import java.util.concurrent.atomic.AtomicInteger

import com.google.common.util.concurrent.AtomicDouble
import org.allenai.common.LoggingWithUncaughtExceptions
import org.allenai.nlpstack.chunk.{defaultChunker => chunker}
import org.allenai.nlpstack.core.PostaggedToken
import org.allenai.nlpstack.core.parse.graph.DependencyGraph

import scala.collection.mutable
import scala.io.Source
import scala.util.Random

trait SimilarityBasedExtractor extends ListExtractor with LoggingWithUncaughtExceptions {
  var simCoeff: Double = 1
  var langCoeff: Double = 0
  var augmentingWindowSize: Int = 5
  def lambda = (simCoeff / (simCoeff + langCoeff), langCoeff / (simCoeff + langCoeff))
  lazy val ruleBasedExtractor = new RuleBasedExtractor
  lazy val langModelWrapper = new LanguageModelWrapper
  lazy val wordVectorWrapper = new WordVectorWrapper
  var DEBUG = false

  def trimModelsToSentences(file: Option[String], inpSents: Option[Seq[String]]) = {
    val sentences = mutable.Set[String]()
    logger.info("Aggregating Sentences")
    if (file.isDefined) {
      val data = Source.fromFile(file.get).getLines()
      val numSentences = data.next().toInt
      for (i <- 0 until numSentences) {
        val sent = data.next()
        sentences.add(sent)
        val listCount = data.next().toInt
        for (j <- 0 until listCount) {
          val Seq(ccId, elemCount) = data.next().split(" ").map(_.toInt).toSeq
          for (k <- 0 until elemCount) {
            val elemSize = data.next().toInt
            val elemRange = data.next().split(" ").map(_.toInt)
          }
        }
      }
    }
    if (inpSents.isDefined) {
      inpSents.get.foreach(sentences.add(_))
    }
    logger.info(s"Aggregated with ${sentences.size} sentences")
    wordVectorWrapper.initialised.set(false)
    langModelWrapper.initialised.set(false)
    wordVectorWrapper.sentences = sentences.toSeq
    langModelWrapper.sentences = sentences.toSeq
  }

  def getSimilarityVector(tokens: Seq[PostaggedToken], listRange: ListRange, params: Params): FeatureVector

  def getSimilarityScore(tokens: Seq[PostaggedToken], listRange: ListRange, params: Params = Params()): Double

  def getLanguageModelScore(tokens: Seq[PostaggedToken], listRange: ListRange): Double = {
    val leftTokens = tokens.slice(0, listRange.elemsRange.head._1).map(_.string)
    val rightTokens = tokens.slice(listRange.elemsRange.last._2 + 1, tokens.size).map(_.string)
    val elemsProb = listRange.elemsRange.map {
      case (x, y) => leftTokens ++ tokens.slice(x, y + 1).map(_.string) ++ rightTokens
    }.map(langModelWrapper.getAverageProb)
    val res = if (elemsProb.isEmpty) 0
    else elemsProb.sum / elemsProb.size.toDouble
    res
  }

  def extractListRange(sentence: String): (Seq[PostaggedToken], DependencyGraph, Seq[ListRange]) = {
    val (tokens, parse, listRanges) = ruleBasedExtractor.extractListRange(sentence)
    val chunks = chunker.chunkPostagged(tokens)
    val augmentedListRanges = mutable.ArrayBuffer[ListRange]()
    for (listRange <- listRanges) {
      val (leftEnd, rightEnd) = (listRange.elemsRange.head, listRange.elemsRange.last)
      val (bestTotalScore, bestLeftIdx, bestRightIdx) = (for {
        i <- (Math.max(0, leftEnd._1 - augmentingWindowSize + 1) to Math.min(leftEnd._2, leftEnd._1 + augmentingWindowSize))
        j <- (Math.max(rightEnd._1, rightEnd._2 - augmentingWindowSize + 1) until Math.min(tokens.size, rightEnd._2 + augmentingWindowSize))
        scoreTuple = if((i == leftEnd._1 || chunks(i).chunk.startsWith("B-")) &&
          (j == rightEnd._2 || j+1 == tokens.size || chunks(j+1).chunk.startsWith("B-"))){
          val augmentedListRange = listRange
          augmentedListRange.elemsRange(0) = (i, augmentedListRange.elemsRange.head._2)
          augmentedListRange.elemsRange(augmentedListRange.elemsRange.size - 1) = (augmentedListRange.elemsRange.last._1, j)
          val params = Params(i - leftEnd._1, j - rightEnd._2)
          val simVector = if (lambda._1 == 0) FeatureVector.Zeros() else getSimilarityVector(tokens, augmentedListRange, params)
          val simScore = if (lambda._1 == 0) 0 else getSimilarityScore(tokens, augmentedListRange, params)
          val langScore = if (lambda._2 == 0) 0 else getLanguageModelScore(tokens, augmentedListRange)
          val totalScore = lambda._1 * simScore + lambda._2 * langScore
          if (DEBUG) logger.info(
            s"ListRange: $augmentedListRange\tParams: ${Params(i - leftEnd._1, j - rightEnd._2)}\tSimVector: $simVector" +
              s"\tSimScore: $simCoeff $simScore\tLangScore: $langCoeff $langScore\tTotalScore: $totalScore"
          )
          Some((totalScore, i, j))
        } else None
      } yield scoreTuple).flatten.max
      listRange.elemsRange(0) = (bestLeftIdx, listRange.elemsRange.head._2)
      listRange.elemsRange(listRange.elemsRange.size - 1) = (listRange.elemsRange.last._1, bestRightIdx)
      listRange.conf = bestTotalScore
      listRange.defaultIndices = Params(leftEnd._1, rightEnd._2)
      augmentedListRanges += listRange
    }
    (tokens, parse, augmentedListRanges)
  }
}
