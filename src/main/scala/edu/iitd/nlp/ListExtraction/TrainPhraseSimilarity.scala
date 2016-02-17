package edu.iitd.nlp.ListExtraction

import java.io._
import org.allenai.common.LoggingWithUncaughtExceptions
import scala.collection.mutable
import scala.io.Source

object TrainPhraseSimilarity extends App with LoggingWithUncaughtExceptions {
  def loadData(file: String): (Seq[(String, Set[String], String)], Seq[(String, Set[String], String)]) = {
    val data = Source.fromFile(file).getLines()
    val trainData = mutable.ArrayBuffer[(String, Set[String], String)]()
    val testData = mutable.ArrayBuffer[(String, Set[String], String)]()
    var (lineCount, hashCount) = (0, 0)
    while(data.hasNext){
      val sent = data.next()
      if(sent.startsWith("##")) hashCount += 1
      if(!sent.startsWith("#")) {
        lineCount += 1
        val lis = sent.split('|').map(_.trim).toSeq
//        if(lineCount <= 4) logger.info(s"$sent $lis $trainData")
        if(hashCount == 4){
          trainData += ((lis.head, lis.drop(1).toSet, lis(1)))
        } else {
          testData += ((lis.head, lis.drop(1).toSet, lis(1)))
        }
      }
    }
    (trainData, testData)
  }

  def calcScore(phraseSimilarity: PhraseSimilarity, data: Seq[(String, Set[String], String)]): Double = {
    val res = data.map {
      case (question, options, ans) =>
        val candAns = phraseSimilarity.evaluate(question, options)
        if(candAns == ans) 1.0
        else 0.0
    }
    if(res.isEmpty) 0
    else res.sum/res.size.toDouble
  }

  val logFileName = "logs/" + this.getClass.getName + ".txt"
  val writer = new PrintWriter(new File(logFileName))
  val phraseSimilarity = new PhraseSimilarity
  val file = "data/live-3640-6413-jair.txt"
  phraseSimilarity.trimModelsToSentences(Some(file), None)
  val (trainData, testData) = loadData(file)
  logger.info(s"Train data size: ${trainData.size} ${trainData.slice(0,3)} Test data size: ${testData.size} ${testData.slice(0,3)}")
  var (testScore, trainScore) = (calcScore(phraseSimilarity, testData), calcScore(phraseSimilarity, trainData))
  logger.info(s"Pre Training:\tFeature Vector: ${phraseSimilarity.weightVector}")
  logger.info(s"Pre Training:\tTrain Score: $trainScore\tTest Score: $testScore")
  val numIter = 1000
  val learningRate = 0.001
  for (iter <- 0 until numIter) {
    trainData.foreach {
      case (question, options, ans) =>
        val candAns = phraseSimilarity.evaluate(question, options)
        if(candAns != ans){
          val fv1 = phraseSimilarity.getPhraseSimilarityVector(question, candAns)
          val fv2 = phraseSimilarity.getPhraseSimilarityVector(question, ans)
          phraseSimilarity.weightVector = phraseSimilarity.weightVector + (fv2 - fv1) * learningRate
        }
    }
    testScore = calcScore(phraseSimilarity, testData)
    trainScore = calcScore(phraseSimilarity, trainData)
    logger.info(s"Iteration $iter:\tFeature Vector: ${phraseSimilarity.weightVector}")
    logger.info(s"Iteration $iter:\tTrain Score: $trainScore\tTest Score: $testScore")
  }
}
