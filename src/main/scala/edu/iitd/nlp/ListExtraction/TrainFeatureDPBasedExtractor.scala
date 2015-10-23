package edu.iitd.nlp.ListExtraction

import org.allenai.common.LoggingWithUncaughtExceptions
import scala.collection.mutable
import scala.io.Source
import org.allenai.nlpstack.tokenize.{ defaultTokenizer => tokenizer }

object TrainFeatureDPBasedExtractor extends App with LoggingWithUncaughtExceptions {
  def loadData: Seq[(String, Seq[ListRange])] = {
    val file = "data/british_news_treebank_dataset"
    val data = Source.fromFile(file).getLines()
    val scorer = new MaxMatchScorer
    val res = mutable.ArrayBuffer[(String, Seq[ListRange])]()
    val numSentences = data.next().toInt

    for (i <- 0 until numSentences) {
      val sent = data.next()
      val sentTokenCount = sent.split(" ").size

      val listCount = data.next().toInt
      val goldListsRange = mutable.ArrayBuffer[ListRange]()
      for (j <- 0 until listCount) {
        val Seq(ccId, elemCount) = data.next().split(" ").map(_.toInt).toSeq
        val elemPos = mutable.ArrayBuffer[(Int, Int)]()
        for (k <- 0 until elemCount) {
          val elemSize = data.next().toInt
          val elemRange = data.next().split(" ").map(_.toInt)
          elemPos += ((elemRange.head, elemRange.last))
        }
        goldListsRange += ListRange(ccId, elemPos, 1.0)
      }
      val tokens = tokenizer.tokenize(sent)
      if (tokens.size == sentTokenCount) res += ((sent, goldListsRange))
    }
    res
  }
  def calcScore(extractor: FeatureDPBasedExtractor, data: Seq[(String, Seq[ListRange])]): Score = {
    val scorer = new MaxMatchScorer
    data.foreach {
      case (sent, goldLists) =>
        val (tokens, parse, candidateLists) = extractor.extractListRange(sent)
        scorer.addSentence(sent, candidateLists, goldLists)
    }
    scorer.getAverageScore
  }
  val extractor = new FeatureDPBasedExtractor(1, 0)
  val availData = loadData.slice(0, 10)
  val (trainData, testData) = availData.splitAt(8 * availData.length / 10)
  var (testScore, trainScore) = (calcScore(extractor, testData), calcScore(extractor, trainData))
  logger.info(s"Pre: Train Score: $trainScore\tTest Score: $testScore")
  val numIter = 10
  val learningRate = 0.1
  for (iter <- 0 until numIter) {
    trainData.foreach {
      case (sent, goldListsRange) =>
        val (tokens, _, candListsRange) = extractor.extractListRange(sent)
        val scorer = new MaxMatchScorer
        val sentResult = scorer.addSentence(sent, candListsRange, goldListsRange)
        sentResult.filter {
          case (_, candL, goldL) => candL.ccPos == goldL.ccPos
        }.foreach {
          case (_, candL, goldL) =>
            val fv1 = extractor.getSimilarityVector(tokens, candL)
            val fv2 = extractor.getSimilarityVector(tokens, goldL)
            extractor.featureVector += (fv2 - fv1) * learningRate
            logger.info(s"FV: ${extractor.featureVector}")
        }
    }
  }
  testScore = calcScore(extractor, testData)
  trainScore = calcScore(extractor, trainData)
  logger.info(s"Feature Vector: ${extractor.featureVector}")
  logger.info(s"Post: Train Score: $trainScore\tTest Score: $testScore")
}
