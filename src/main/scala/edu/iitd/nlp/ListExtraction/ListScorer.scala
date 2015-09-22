package edu.iitd.nlp.ListExtraction

import scala.collection.mutable

case class Score(precision: Double, recall: Double) {
  def +(other: Score): Score = {
    Score(precision + other.precision, recall + other.recall)
  }

  def /(num: Double): Score = {
    Score(precision / num, recall / num)
  }

  def <(other: Score): Boolean = {
    precision < other.precision
  }

  def >(other: Score): Boolean = {
    ! <(other)
  }
}

object ScoreImplicits {
  implicit val implicitOrdering = new Ordering[Score] {
    def compare(a: Score, b: Score): Int = Math.signum(a.precision - b.precision).toInt
  }
}

trait ListScorer {
  def scoreList(candidateList: ListRange, goldList: ListRange): Score
  def addSentence(sentence: String, candidateLists: Seq[ListRange], goldLists: Seq[ListRange]): Unit
  def getScoreVector: Seq[(String, ListRange, Score)]
  def getLastScore: Score
  def getAverageScore: Score
}

class MaxMatchScorer extends ListScorer {
  val scoreVector = mutable.ArrayBuffer.empty[(String, ListRange, Score)]
  var scoreSum = Score(0, 0)

  def rangeIntersection(a: (Int, Int), b: (Int, Int)): Int = {
    Math.max(0, Math.min(a._2, b._2) - Math.max(a._1, b._1) + 1)
  }

  def rangeSize(a: (Int, Int)): Int = a._2 - a._1 + 1

  def scoreList(candList: ListRange, goldList: ListRange): Score = {
    if (candList.ccPos != goldList.ccPos) Score(0, 0)
    else {
      val elemScores = for {
        candElem <- candList.elemsRange
        candSize = rangeSize(candElem)
        (maxIntersection, goldSize) = goldList.elemsRange.map(g => (rangeIntersection(candElem, g), rangeSize(g))).max
        precision = maxIntersection.toDouble / candSize.toDouble
        recall = maxIntersection.toDouble / goldSize.toDouble
        score = Score(precision, recall)
      } yield score
      val avgPrecision = elemScores.map(_.precision).sum / candList.elemsRange.size.toDouble
      val avgRecall = elemScores.map(_.recall).sum / goldList.elemsRange.size.toDouble
      Score(avgPrecision, avgRecall)
    }
  }

  import ScoreImplicits._
  def addSentence(sentence: String, candidateLists: Seq[ListRange], goldLists: Seq[ListRange]): Unit = {
    candidateLists.foreach {
      case candList =>
        val maxScore = goldLists.map(scoreList(candList, _)).max
        scoreVector += ((sentence, candList, maxScore))
        scoreSum = scoreSum + maxScore
    }
  }

  def getScoreVector = scoreVector.toSeq

  def getLastScore = scoreVector.last._3
  def getAverageScore = scoreSum / scoreVector.size.toDouble
}
