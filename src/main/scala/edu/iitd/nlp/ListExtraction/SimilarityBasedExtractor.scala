package edu.iitd.nlp.ListExtraction

import java.util.concurrent.atomic.AtomicInteger

import com.google.common.util.concurrent.AtomicDouble
import org.allenai.common.LoggingWithUncaughtExceptions
import org.allenai.nlpstack.core.PostaggedToken
import org.allenai.nlpstack.core.parse.graph.DependencyGraph

import scala.collection.mutable

trait SimilarityBasedExtractor extends ListExtractor {
  var simCoeff: Double = 1
  var langCoeff: Double = 0
  var augmentingWindowSize: Int = 5
  val lambda = (simCoeff / (simCoeff + langCoeff), langCoeff / (simCoeff + langCoeff))
  lazy val ruleBasedExtractor = new RuleBasedExtractor
  lazy val langModelWrapper = new LanguageModelWrapper
  lazy val wordVectorWrapper = new WordVectorWrapper
  var DEBUG = false

  def getSimilarityVector(tokens: Seq[PostaggedToken], listRange: ListRange, params: Params): FeatureVector

  def getSimilarityScore(tokens: Seq[PostaggedToken], listRange: ListRange, params: Params = Params()): Double

  def getLanguageModelScore(tokens: Seq[PostaggedToken], listRange: ListRange): Double = 0
  //  {
  //    val leftTokens = tokens.slice(0, listRange.elemsRange.head._1).map(_.string)
  //    val rightTokens = tokens.slice(listRange.elemsRange.last._2 + 1, tokens.size).map(_.string)
  //    val elemsProb = listRange.elemsRange.map {
  //      case (x, y) => leftTokens ++ tokens.slice(x, y + 1).map(_.string) ++ rightTokens
  //    }.map(langModelWrapper.getAverageProb)
  //    val res = if (elemsProb.isEmpty) 0
  //    else elemsProb.sum / elemsProb.size.toDouble
  //    res
  //  }

  def extractListRange(sentence: String): (Seq[PostaggedToken], DependencyGraph, Seq[ListRange]) = {
    val (tokens, parse, listRanges) = ruleBasedExtractor.extractListRange(sentence)
    val augmentedListRanges = mutable.ArrayBuffer[ListRange]()
    for (listRange <- listRanges) {
      val (leftEnd, rightEnd) = (listRange.elemsRange.head, listRange.elemsRange.last)
      val (bestTotalScore, bestLeftIdx, bestRightIdx) = (for {
        i <- (Math.max(0, leftEnd._1 - augmentingWindowSize + 1) to Math.min(leftEnd._2, leftEnd._1 + augmentingWindowSize))
        j <- (Math.max(rightEnd._1, rightEnd._2 - augmentingWindowSize + 1) until Math.min(tokens.size, rightEnd._2 + augmentingWindowSize))
        augmentedListRange = listRange
        _ = augmentedListRange.elemsRange(0) = (i, augmentedListRange.elemsRange.head._2)
        _ = augmentedListRange.elemsRange(augmentedListRange.elemsRange.size - 1) = (augmentedListRange.elemsRange.last._1, j)
        params = Params(i - leftEnd._1, j - rightEnd._2)
        simVector = if (lambda._1 == 0) FeatureVector.Zeros() else getSimilarityVector(tokens, augmentedListRange, params)
        simScore = if (lambda._1 == 0) 0 else getSimilarityScore(tokens, augmentedListRange, params)
        langScore = if (lambda._2 == 0) 0 else getLanguageModelScore(tokens, augmentedListRange)
        totalScore = lambda._1 * simScore + lambda._2 * langScore
        _ = if (DEBUG) logger.info(
          s"ListRange: $augmentedListRange\tParams: ${Params(i - leftEnd._1, j - rightEnd._2)}\tSimVector: $simVector" +
            s"\tSimScore: $simScore\tLangScore: $langScore\tTotalScore: $totalScore"
        )
        scoreTuple = (totalScore, i, j)
      } yield scoreTuple).max
      listRange.elemsRange(0) = (bestLeftIdx, listRange.elemsRange.head._2)
      listRange.elemsRange(listRange.elemsRange.size - 1) = (listRange.elemsRange.last._1, bestRightIdx)
      listRange.conf = bestTotalScore
      augmentedListRanges += listRange
    }
    (tokens, parse, augmentedListRanges)
  }
}
