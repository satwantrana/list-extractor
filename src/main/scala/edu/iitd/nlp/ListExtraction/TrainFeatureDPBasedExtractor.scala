package edu.iitd.nlp.ListExtraction

import java.io._
import org.allenai.common.LoggingWithUncaughtExceptions
import scala.collection.mutable
import scala.io.Source
import tokenize.{ defaultTokenizer => tokenizer }

object TrainFeatureDPBasedExtractor extends App with LoggingWithUncaughtExceptions {
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
  val pennTreeBankDataFile = "data/penn_treebank_dataset"
  val britishNewsTreeBankDataFile = "data/british_news_treebank_dataset"
  val dataFile = britishNewsTreeBankDataFile
  val logFileName = "logs/" + this.getClass.getName + ".txt"
  val writer = new PrintWriter(new File(logFileName))
  val ruleBasedExtractor = new RuleBasedExtractor
  val extractor = new FeatureDPBasedExtractor(1, 0)
  extractor.trimModelsToSentences(Some(dataFile), None)
  val availData = loadData(dataFile)
  val (trainData, testData) = availData.splitAt(8 * availData.length / 10)
  val (ruleBasedTestScore, ruleBasedTrainScore) = (
    calcScore(ruleBasedExtractor, testData),
    calcScore(ruleBasedExtractor, trainData)
  )
  logger.info(s"Rule Based: Train Score: $ruleBasedTrainScore\tTest Score: $ruleBasedTestScore")
  var (testScore, trainScore) = (calcScore(extractor, testData), calcScore(extractor, trainData))
  var (bestTestScore, bestTrainScore, bestWeightVector) = (Score(0,0), Score(0,0), FeatureVector.Zeros())
  logger.info(s"Pre Training:\tFeature Vector: ${extractor.weightVector}")
  logger.info(s"Pre Training:\tTrain Score: $trainScore\tTest Score: $testScore")
  val numIter = 1000
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
            val (i, j) = (candL.elemsRange.head._1, candL.elemsRange.last._2)
            val (l, r) = (goldL.elemsRange.head._1, goldL.elemsRange.last._2)
            val (p, q) = (candL.defaultIndices.leftDis, candL.defaultIndices.rightDis)
            val fv1 = extractor.getSimilarityVector(tokens, candL, Params(i - p, j - q))
            val fv2 = extractor.getSimilarityVector(tokens, goldL, Params(l - p, r - q))
            if(1 == 0 && (i != l || j != r)) {
              logger.info(s"$sent $candL $goldL $fv1 $fv2")
            }
            extractor.weightVector = extractor.weightVector + (fv2 - fv1) * learningRate
        }
    }
    testScore = calcScore(extractor, testData)
    trainScore = calcScore(extractor, trainData)
    if(trainScore > bestTrainScore){
      bestTrainScore = trainScore
      bestTestScore = testScore
      bestWeightVector = extractor.weightVector
    }
    logger.info(s"Iteration $iter:\tWeight Vector: ${extractor.weightVector}")
    logger.info(s"Iteration $iter:\tTrain Score: $trainScore\tTest Score: $testScore")
    logger.info(s"Iteration $iter:\tBest Train Score: $bestTrainScore\tCorresponding Test Score: $bestTestScore\tCorresponding Weight Vector: $bestWeightVector")
  }
}
