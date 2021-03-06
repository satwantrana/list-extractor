package edu.iitd.nlp.ListExtraction

import org.allenai.common.LoggingWithUncaughtExceptions
import org.scalatest._
import java.io._
import scala.collection.mutable
import scala.io.Source
import scala.util.Random

class TestRuleBasedExtractor extends FlatSpec with LoggingWithUncaughtExceptions {
  val extractor = new RuleBasedExtractor

  "RuleBasedExtractor" should "run correctly on a simple sentence" ignore {
    val sent = "I like playing hockey, cricket and football."
    val (tokens, parse, listRanges) = extractor.extractListRange(sent)
    val goldListRanges = Seq(ListRange(6, mutable.ArrayBuffer((3, 3), (5, 5), (7, 7)), 1.0))

    assert(listRanges == goldListRanges)
  }

  it should "give correct score on a simple sentence with MaxMatchScorer" ignore {
    val sent = "I like playing hockey, cricket and football."
    val (tokens, parse, listRanges) = extractor.extractListRange(sent)
    val goldListRanges = Seq(ListRange(6, mutable.ArrayBuffer((3, 3), (5, 5), (7, 7)), 1.0))
    val scorer = new MaxMatchScorer
    scorer.addSentence(sent, listRanges, goldListRanges)
    val score = scorer.getAverageScore
    logger.info(s"Cand: $listRanges\nGold: $goldListRanges\nScore: ${scorer.getAverageScore}")
    assert(score == Score(1, 1))
  }

  it should "give >= 70% score on British News Tree Bank dataset with MaxMatchScorer" in {
    val file = "data/british_news_treebank_dataset"
    val data = Source.fromFile(file).getLines()
    val scorer = new MaxMatchScorer

    var skippedSentencesCount = 0
    val numSentences = data.next().toInt
    val listPrintProb = 0.01
    val r = new Random(0L)

    val logFileName = "logs/" + this.getClass.getName + ".txt"
    val writer = new PrintWriter(new File(logFileName))

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
        writer.write(s"Sentence: $sent\n\nGold Lists Range: $goldListsRange\nGold Lists: $goldLists\n\n" +
          s"Matched Gold Lists Range: $matchedGoldListsRange\nMatched Gold Lists: $matchedGoldLists\n\n" +
          s"Candidate Lists Range: $candListsRange\nCandidate Lists: $candLists\n\nScores: $scores\n\n\n")
      }
    }

    val avgScore = scorer.getAverageScore
    val avgScoreByLength = scorer.getAverageScoreByLength.toSeq.sortBy(_._1)
    logger.info(s"Average score on British News Tree Bank dataset: $avgScore with $skippedSentencesCount sentences skipped")
    logger.info(s"Average score by max length of list elements: $avgScoreByLength")
    assert(avgScore.precision >= 0.7)
  }

  it should "give >= 70% score on Penn Tree Bank dataset with MaxMatchScorer" ignore {
    val file = "data/penn_treebank_dataset"
    val data = Source.fromFile(file).getLines()
    val scorer = new MaxMatchScorer

    var skippedSentencesCount = 0
    val numSentences = data.next().toInt
    val listPrintProb = 0.01
    val r = new Random(0L)

    val logFileName = "logs/" + this.getClass.getName + ".txt"
    val writer = new PrintWriter(new FileOutputStream(new File(logFileName), true))

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
        writer.write(s"Sentence: $sent\n\nGold Lists Range: $goldListsRange\nGold Lists: $goldLists\n\n" +
          s"Matched Gold Lists Range: $matchedGoldListsRange\nMatched Gold Lists: $matchedGoldLists\n\n" +
          s"Candidate Lists Range: $candListsRange\nCandidate Lists: $candLists\n\nScores: $scores\n\n\n")
      }
    }

    val avgScore = scorer.getAverageScore
    val avgScoreByLength = scorer.getAverageScoreByLength.toSeq.sortBy(_._1)
    logger.info(s"Average score on British News Tree Bank dataset: $avgScore with $skippedSentencesCount sentences skipped")
    logger.info(s"Average score by max length of list elements: $avgScoreByLength")
    assert(avgScore.precision >= 0.7)
  }
}
