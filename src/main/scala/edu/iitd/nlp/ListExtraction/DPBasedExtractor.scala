package edu.iitd.nlp.ListExtraction

import java.util.concurrent.atomic.AtomicInteger

import com.google.common.util.concurrent.AtomicDouble
import org.allenai.common.LoggingWithUncaughtExceptions
import org.allenai.nlpstack.core.PostaggedToken
import org.allenai.nlpstack.core.parse.graph.DependencyGraph

import scala.collection.mutable

class DPBasedExtractor(var lambda: (Double, Double) = (0,1)) extends ListExtractor{
  lambda = (lambda._1/(lambda._1 + lambda._2), lambda._2/(lambda._1 + lambda._2))
  val ruleBasedExtractor = new RuleBasedExtractor
  val langModelWrapper = new LanguageModelWrapper
  def getSimilarityScore(tokens: Seq[PostaggedToken], listRange: ListRange): Double = {
    0
  }

  def getLanguageModelScore(tokens: Seq[PostaggedToken], listRange: ListRange): Double = {
    val leftTokens = tokens.slice(0,listRange.elemsRange.head._1).map(_.string)
    val rightTokens = tokens.slice(listRange.elemsRange.last._2+1,tokens.size).map(_.string)
    val elemsProb = listRange.elemsRange.map{
        case (x,y) => leftTokens ++  tokens.slice(x,y+1).map(_.string) ++ rightTokens
      }.map(langModelWrapper.computeAverageProb)
    elemsProb.sum / elemsProb.size.toDouble
  }

  def extractListRange(sentence: String): (Seq[PostaggedToken], DependencyGraph, Seq[ListRange]) = {
    val (tokens, parse, listRanges) = ruleBasedExtractor.extractListRange(sentence)
    val augmentedListRanges = mutable.ArrayBuffer[ListRange]()
    for(listRange <- listRanges){
      val (leftEnd, rightEnd) = (listRange.elemsRange.head._1, listRange.elemsRange.last._2)
      val (bestTotalScore, bestLeftIdx, bestRightIdx) =
        (new AtomicDouble(Double.NegativeInfinity), new AtomicInteger(leftEnd), new AtomicInteger(rightEnd))
      for(
        i <- (0 to leftEnd).par;
        j <- (rightEnd until tokens.size).par
      ){
        val augmentedListRange = listRange
        augmentedListRange.elemsRange(0) = (i, augmentedListRange.elemsRange.head._2)
        augmentedListRange.elemsRange(augmentedListRange.elemsRange.size-1) = (augmentedListRange.elemsRange.last._1, j)
        val simScore = getSimilarityScore(tokens, augmentedListRange)
        val langScore = getLanguageModelScore(tokens, augmentedListRange)
        val totalScore = lambda._1*simScore + lambda._2*langScore
        if(totalScore > bestTotalScore.get){
          bestTotalScore.set(totalScore)
          bestLeftIdx.set(i)
          bestRightIdx.set(j)
        }
      }
      listRange.elemsRange(0) = (bestLeftIdx.get, listRange.elemsRange.head._2)
      listRange.elemsRange(listRange.elemsRange.size-1) = (listRange.elemsRange.last._1, bestRightIdx.get)
      listRange.conf = bestTotalScore.get
      augmentedListRanges += listRange
    }
    (tokens, parse, augmentedListRanges)
  }
}

object DPBasedExtractorMain extends LoggingWithUncaughtExceptions with App {
  val sent = "I like playing hockey, cricket and football."
  logger.info(s"Sentence: $sent")

  val extractor = new DPBasedExtractor
  val (tokens, parse, listRanges) = extractor.extractListRange(sent)
  val lists = extractor.extractLists(tokens, listRanges)
  logger.info(s"Tokens: $tokens\nParse Tree: $parse\nLists: $lists\n")

  val scorer = new MaxMatchScorer
  val goldListRanges = Seq(ListRange(6, mutable.ArrayBuffer((3, 3), (5, 5), (7, 7)), 1.0))
  scorer.addSentence(sent, listRanges, goldListRanges)
  logger.info(s"Cand: $listRanges\nGold: $goldListRanges\nScore: ${scorer.getAverageScore}")
}
