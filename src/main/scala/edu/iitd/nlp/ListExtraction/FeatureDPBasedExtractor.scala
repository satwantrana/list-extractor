package edu.iitd.nlp.ListExtraction

import java.util.concurrent.atomic.AtomicInteger
import com.google.common.util.concurrent.AtomicDouble
import org.allenai.common.LoggingWithUncaughtExceptions
import org.allenai.nlpstack.core.{ ChunkedToken, PostaggedToken }
import org.allenai.nlpstack.core.parse.graph.DependencyGraph
import org.allenai.nlpstack.chunk.{ defaultChunker => chunker }

import scala.collection.mutable

case class FeatureVector(vec: mutable.ArrayBuffer[Double] = FeatureVector.default.vec) {
  def +(other: FeatureVector): FeatureVector = {
    FeatureVector(vec.zip(other.vec).map { case (a, b) => a + b })
  }

  def -(other: FeatureVector): FeatureVector = {
    FeatureVector(vec.zip(other.vec).map { case (a, b) => a - b })
  }

  def *(other: FeatureVector): Double = {
    vec.zip(other.vec).map { case (a, b) => a * b }.sum
  }

  def *(mult: Double): FeatureVector = {
    FeatureVector(vec.map(_ * mult))
  }

  def /(div: Double): FeatureVector = {
    FeatureVector(vec.map(_ / div))
  }
}

object FeatureVector {
  val default = FeatureVector(mutable.ArrayBuffer(0.0, 1.0, 0.0, 0.0))
  val zeros = FeatureVector(mutable.ArrayBuffer.fill(4)(0.0))
  val ninfies = FeatureVector(mutable.ArrayBuffer.fill(4)(Double.NegativeInfinity))
}

class FeatureDPBasedExtractor(simCoeff: Double = 1, langCoeff: Double = 1,
    augmentingWindowSize: Int = 5, var weightVector: FeatureVector = FeatureVector.default) extends ListExtractor {
  val lambda = (simCoeff / (simCoeff + langCoeff), langCoeff / (simCoeff + langCoeff))
  lazy val ruleBasedExtractor = new RuleBasedExtractor
  lazy val langModelWrapper = new LanguageModelWrapper
  lazy val wordVectorWrapper = new WordVectorWrapper

  def getSimilarityVector(tokens: Seq[PostaggedToken], listRange: ListRange): FeatureVector = {
    val chunks = chunker.chunkPostagged(tokens)
    val elems = listRange.elemsRange.map {
      case (x, y) => chunks.slice(x, y + 1)
    }.sliding(2).toList
    val elemsSim = elems.map {
      case l => wordVectorWrapper.getFeatureDPPhraseSimilarity(l(0), l(1), weightVector)
    }
    elemsSim.foldLeft(FeatureVector.zeros) { case (a, b) => a + b } / elemsSim.size.toDouble
  }

  def getSimilarityScore(tokens: Seq[PostaggedToken], listRange: ListRange): Double = {
    val chunks = chunker.chunkPostagged(tokens)
    val elems = listRange.elemsRange.map {
      case (x, y) => chunks.slice(x, y + 1)
    }.sliding(2).toList
    val elemsSim = elems.map {
      case l => weightVector * wordVectorWrapper.getFeatureDPPhraseSimilarity(l(0), l(1), weightVector)
    }
    val res = if (elemsSim.isEmpty) 0
    else elemsSim.sum / elemsSim.size.toDouble
    res
  }

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
    val augmentedListRanges = mutable.ArrayBuffer[ListRange]()
    val chunks = chunker.chunkPostagged(tokens)
    for (listRange <- listRanges) {
      val (leftEnd, rightEnd) = (listRange.elemsRange.head, listRange.elemsRange.last)
      val (bestTotalScore, bestLeftIdx, bestRightIdx) =
        (new AtomicDouble(Double.NegativeInfinity), new AtomicInteger(leftEnd._1), new AtomicInteger(rightEnd._2))
      for (
        i <- (Math.max(0, leftEnd._1 - augmentingWindowSize + 1) to Math.min(leftEnd._2, leftEnd._1 + augmentingWindowSize)).par;
        j <- (Math.max(rightEnd._1, rightEnd._2 - augmentingWindowSize + 1) until Math.min(tokens.size, rightEnd._2 + augmentingWindowSize)).par
      ) {
        val augmentedListRange = listRange
        augmentedListRange.elemsRange(0) = (i, augmentedListRange.elemsRange.head._2)
        augmentedListRange.elemsRange(augmentedListRange.elemsRange.size - 1) = (augmentedListRange.elemsRange.last._1, j)
        val simScore = if (lambda._1 == 0) 0 else getSimilarityScore(tokens, augmentedListRange)
        val langScore = if (lambda._2 == 0) 0 else getLanguageModelScore(tokens, augmentedListRange)
        val totalScore = lambda._1 * simScore + lambda._2 * langScore
        //        logger.info(
        //          s"ListRange: $augmentedListRange\tSimScore: $simScore\tLangScore: $langScore\tTotalScore: $totalScore"
        //        )
        if (totalScore > bestTotalScore.get) {
          bestTotalScore.set(totalScore)
          bestLeftIdx.set(i)
          bestRightIdx.set(j)
        }
      }
      listRange.elemsRange(0) = (bestLeftIdx.get, listRange.elemsRange.head._2)
      listRange.elemsRange(listRange.elemsRange.size - 1) = (listRange.elemsRange.last._1, bestRightIdx.get)
      listRange.conf = bestTotalScore.get
      augmentedListRanges += listRange
    }
    (tokens, parse, augmentedListRanges)
  }
}

object FeatureDPBasedExtractorMain extends LoggingWithUncaughtExceptions with App {
  val sent = "I like playing hockey, cricket and football."
  logger.info(s"Sentence: $sent")

  val extractor = new FeatureDPBasedExtractor(1, 0)
  val (tokens, parse, listRanges) = extractor.extractListRange(sent)
  val lists = extractor.extractLists(tokens, listRanges)
  logger.info(s"Tokens: $tokens\nParse Tree: $parse\nLists: $lists\n")

  val scorer = new MaxMatchScorer
  val goldListRanges = Seq(ListRange(6, mutable.ArrayBuffer((3, 3), (5, 5), (7, 7)), 1.0))
  scorer.addSentence(sent, listRanges, goldListRanges)
  logger.info(s"Cand: $listRanges\nGold: $goldListRanges\nScore: ${scorer.getAverageScore}")
}
