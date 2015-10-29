package edu.iitd.nlp.ListExtraction

import java.util.concurrent.atomic.AtomicInteger

import com.google.common.util.concurrent.AtomicDouble
import org.allenai.common.LoggingWithUncaughtExceptions
import org.allenai.nlpstack.chunk._
import org.allenai.nlpstack.core.PostaggedToken
import org.allenai.nlpstack.core.parse.graph.DependencyGraph

import scala.collection.mutable

class DPBasedExtractor(_simCoeff: Double = 1, _langCoeff: Double = 1, _augmentingWindowSize: Int = 5) extends SimilarityBasedExtractor {
  simCoeff = _simCoeff
  langCoeff = _langCoeff
  augmentingWindowSize = _augmentingWindowSize

  def getSimilarityVector(tokens: Seq[PostaggedToken], listRange: ListRange, params: Params): FeatureVector = {
    val res = getSimilarityScore(tokens, listRange, params)
    FeatureVector(mutable.ArrayBuffer(res))
  }

  def getSimilarityScore(tokens: Seq[PostaggedToken], listRange: ListRange, params: Params = Params()): Double = {
    val elems = listRange.elemsRange.map {
      case (x, y) => tokens.slice(x, y + 1).map(_.string)
    }.sliding(2).toList
    val elemsSim = elems.map {
      case l => wordVectorWrapper.getDPPhraseSimilarity(l(0), l(1))
    }
    val res = if (elemsSim.isEmpty) 0
    else elemsSim.sum / elemsSim.size.toDouble
    res
  }
}

object DPBasedExtractorMain extends LoggingWithUncaughtExceptions with App {
  val sent = "I like playing hockey, cricket and football."
  logger.info(s"Sentence: $sent")

  val extractor = new DPBasedExtractor(1, 0)
  val (tokens, parse, listRanges) = extractor.extractListRange(sent)
  val lists = extractor.extractLists(tokens, listRanges)
  logger.info(s"Tokens: $tokens\nParse Tree: $parse\nLists: $lists\n")

  val scorer = new MaxMatchScorer
  val goldListRanges = Seq(ListRange(6, mutable.ArrayBuffer((3, 3), (5, 5), (7, 7)), 1.0))
  scorer.addSentence(sent, listRanges, goldListRanges)
  logger.info(s"Cand: $listRanges\nGold: $goldListRanges\nScore: ${scorer.getAverageScore}")
}
