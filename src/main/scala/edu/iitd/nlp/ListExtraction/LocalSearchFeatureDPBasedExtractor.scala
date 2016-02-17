package edu.iitd.nlp.ListExtraction

import java.io._

import org.allenai.common.LoggingWithUncaughtExceptions
import tokenize.{ defaultTokenizer => tokenizer }

import scala.collection.mutable
import scala.io.Source
import scala.util.Random

object LocalSearchFeatureDPBasedExtractor extends App with LoggingWithUncaughtExceptions {
  def loadData(file: String): Seq[(String, Seq[ListRange])] = {
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

  def calcScore(extractor: ListExtractor, data: Seq[(String, Seq[ListRange])]): Score = {
    val scorer = new MaxMatchScorer
    data.foreach {
      case (sent, goldLists) =>
        val (tokens, parse, candidateLists) = extractor.extractListRange(sent)
        scorer.addSentence(sent, candidateLists, goldLists)
    }
    scorer.getAverageScore
  }
  val logFileName = "logs/" + this.getClass.getName + ".txt"
  val writer = new PrintWriter(new File(logFileName))
  val ruleBasedExtractor = new RuleBasedExtractor
  val extractor = new FeatureDPBasedExtractor(1, 0)
  val file = "data/british_news_treebank_dataset"
  extractor.trimModelsToSentences(Some(file), None)
  extractor.weightVector = FeatureVector.baseLine()
  val availData = loadData(file)
  val (trainData, testData) = availData.splitAt(8 * availData.length / 10)
  val (ruleBasedTestScore, ruleBasedTrainScore) = (
    calcScore(ruleBasedExtractor, testData),
    calcScore(ruleBasedExtractor, trainData)
  )
  logger.info(s"Rule Based: Train Score: $ruleBasedTrainScore\tTest Score: $ruleBasedTestScore")
  var (testScore, trainScore) = (calcScore(extractor, testData), calcScore(extractor, trainData))
  var (bestTestScore, bestTrainScore, bestWeightVector) = (testScore, trainScore, extractor.weightVector)
  logger.info(s"Pre Training:\tFeature Vector: ${extractor.weightVector}")
  logger.info(s"Pre Training:\tTrain Score: $trainScore\tTest Score: $testScore")
  val numIter = 1000
  val incRate = 0.1
  for (iter <- 0 until numIter) {
    val r = new Random
    val restartProb = 0.1
    if (r.nextDouble() < restartProb) extractor.weightVector =
      FeatureVector(mutable.ArrayBuffer.fill(FeatureVector.defaultNumFeatures)((r.nextDouble() - 0.5) * 2.0 / incRate))
    val diff = FeatureVector(mutable.ArrayBuffer.fill(FeatureVector.defaultNumFeatures)((r.nextDouble() - 0.5) * 2.0 * incRate))
    extractor.weightVector += diff
    testScore = calcScore(extractor, testData)
    trainScore = calcScore(extractor, trainData)
    val hillClimbProb = 0.3
    if (r.nextDouble() > hillClimbProb && trainScore < bestTrainScore) extractor.weightVector -= diff
    else if (trainScore > bestTrainScore) {
      bestTestScore = testScore
      bestTrainScore = trainScore
      bestWeightVector = extractor.weightVector
    }
    logger.info(s"Iteration $iter:\tFeature Vector: ${extractor.weightVector}")
    logger.info(s"Iteration $iter:\tTrain Score: $trainScore\tTest Score: $testScore")
    logger.info(s"Iteration $iter:\tBest Train Score: $bestTrainScore\tTest Score: $bestTestScore\tWeight Vector: $bestWeightVector")
  }
}
