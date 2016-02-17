package edu.iitd.nlp.ListExtraction

import org.allenai.common.LoggingWithUncaughtExceptions

import scala.collection.mutable
import scala.io.Source
import scala.util.Random

object RandomGridSearchDPBasedExtractor extends App with LoggingWithUncaughtExceptions {
  val extractor = new DPBasedExtractor(1, 0)
  val file = "data/british_news_treebank_dataset"
  extractor.trimModelsToSentences(Some(file), None)

  val coeffSet = Seq[(Double, Double)]((1, 0), (1, 0.25), (1, 0.5), (1, 1), (1, 2), (1, 4), (1, 0))
  var (bestScore, bestCoeff) = (Score(0, 0), (0d, 0d))

  val numIter = 1000
  val incRate = 0.1

  for (iter <- 0 until numIter) {
    val r = new Random
    val restartProb = 0.1
    val diff = ((r.nextDouble()-0.5)*2*incRate, (r.nextDouble()-0.5)*2*incRate)

    if(iter < coeffSet.size){
      extractor.simCoeff = coeffSet(iter)._1
      extractor.langCoeff = coeffSet(iter)._2
    } else if(iter == coeffSet.size){
      extractor.simCoeff = bestCoeff._1
      extractor.langCoeff = bestCoeff._2
    }
    else if(r.nextDouble() < restartProb || extractor.simCoeff < 0 || extractor.langCoeff < 0){
      extractor.simCoeff = r.nextDouble()
      extractor.langCoeff = r.nextDouble()
    } else {
      extractor.simCoeff += diff._1
      extractor.langCoeff += diff._2
    }

    val data = Source.fromFile(file).getLines()
    val scorer = new MaxMatchScorer

    var skippedSentencesCount = 0
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
      val (tokens, parse, candListsRange) = extractor.extractListRange(sent)
      if (tokens.size != sentTokenCount) skippedSentencesCount += 1
      else {
        val sentResult = scorer.addSentence(sent, candListsRange, goldListsRange)
        val matchedGoldListsRange = sentResult.map(_._3)
        val scores = sentResult.map(_._1)
        val goldLists = extractor.extractLists(tokens, goldListsRange)
        val matchedGoldLists = extractor.extractLists(tokens, matchedGoldListsRange)
        val candLists = extractor.extractLists(tokens, candListsRange)
      }
    }

    val avgScore = scorer.getAverageScore
    val hillClimbProb = 0.3
    if (r.nextDouble() > hillClimbProb && avgScore < bestScore){
      extractor.simCoeff -= diff._1
      extractor.langCoeff -= diff._2
    }
    else if (avgScore > bestScore) {
      bestScore = avgScore
      bestCoeff = (extractor.simCoeff, extractor.langCoeff)
    }

    logger.info(s"Average score on British News Tree Bank dataset: $avgScore with params ${(extractor.simCoeff, extractor.langCoeff)}")
    logger.info(s"Best average score on British News Tree Bank dataset: $bestScore with params ${bestCoeff}")
  }
  logger.info(s"Best average score on British News Tree Bank dataset: $bestScore with params ${bestCoeff}")
}
