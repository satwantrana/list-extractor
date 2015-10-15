package edu.iitd.nlp.ListExtraction

import org.allenai.common.LoggingWithUncaughtExceptions
import org.scalatest._

import scala.collection.mutable
import scala.io.Source
import scala.util.Random

class TestDPBasedExtractor extends FlatSpec with LoggingWithUncaughtExceptions {
  val extractor = new DPBasedExtractor(1, 0)

  "DPBasedExtractor" should "run correctly on a simple sentence" in {
    val sent = "I like playing hockey, cricket and football."
    val (tokens, parse, listRanges) = extractor.extractListRange(sent)
    val goldListRanges = Seq(ListRange(6, mutable.ArrayBuffer((3, 3), (5, 5), (7, 7)), 1.0))

    assert(listRanges.map(l => ListRange(l.ccPos, l.elemsRange, 1.0)) == goldListRanges)
  }

  it should "give correct score on a simple sentence with MaxMatchScorer" in {
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
        if (r.nextDouble() < listPrintProb) {
          val goldLists = extractor.extractLists(tokens, goldListsRange)
          val candLists = extractor.extractLists(tokens, candListsRange)
          val score = scorer.getLastScore
          logger.debug(s"Sentence: $sent\nGold Lists: $goldLists\nCandidate Lists: $candLists\nScore: $score")
        }
        scorer.addSentence(sent, candListsRange, goldListsRange)
      }
    }

    val avgScore = scorer.getAverageScore
    logger.info(s"Average score on British News Tree Bank dataset: $avgScore with $skippedSentencesCount sentences skipped")

    assert(avgScore.precision >= 0.7)
  }
}
