package edu.iitd.nlp.ListExtraction

import org.allenai.common.LoggingWithUncaughtExceptions
import org.scalatest._

import scala.collection.mutable

class TestMaxMatchScorer extends FlatSpec with LoggingWithUncaughtExceptions {
  "MaxMatchScorer" should "not run correctly on a wrong match." in {
    val scorer = new MaxMatchScorer
    val candList = ListRange(25, mutable.ArrayBuffer((21, 24), (26, 30)), 0.03895848622944061)
    val goldList = ListRange(25, mutable.ArrayBuffer((24, 24), (26, 26)), 1.0)
    val score = scorer.scoreList(candList, goldList)
    logger.info(s"Score: $score")
    assert(score != Score(1.0, 1.0))
  }

  it should "work correctly on multiple sentences." in {
    val scorer = new MaxMatchScorer
    val sent = "Its weakness was its technical conservatism ; although in 1880 the Admiralty agreed to reintroduce breechloading guns on heavy ships , the armored cruisers Imperieuse and Warspite , which were laid down in the same year , were still designed to carry a full spread of sail ."
    val candList = mutable.ArrayBuffer(ListRange(25, mutable.ArrayBuffer((21, 24), (26, 30)), 0.03895848622944061))
    val goldList = mutable.ArrayBuffer(ListRange(25, mutable.ArrayBuffer((24, 24), (26, 26)), 1.0))
    scorer.addSentence(sent, candList, goldList)
    scorer.addSentence(sent, candList, goldList)
    val scoreVector = scorer.getScoreVector
    logger.info(s"Score Vector: $scoreVector")
    assert(scoreVector.head == scoreVector.last)
  }
}
