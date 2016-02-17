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
  def addSentence(sentence: String, candidateLists: Seq[ListRange], goldLists: Seq[ListRange]): Seq[(Score, ListRange, ListRange)]
  def getScoreVector: Seq[(String, ListRange, ListRange, Score)]
  def getAverageScore: Score
}

class MaxMatchScorer extends ListScorer {
  val scoreVector = mutable.ArrayBuffer.empty[(String, ListRange, ListRange, Score)]
  val scoreSumByLength = mutable.Map[Int, (Score, Int)]()
  var scoreSum = Score(0, 0)

  def rangeIntersection(a: (Int, Int), b: (Int, Int)): Int = {
    Math.max(0, Math.min(a._2, b._2) - Math.max(a._1, b._1) + 1)
  }

  def rangeSize(a: (Int, Int)): Int = a._2 - a._1 + 1

  def scoreList(candList: ListRange, goldList: ListRange): Score = {
    def calcAccuracy(candList: ListRange, goldList: ListRange): Double = {
      if (candList.ccPos != goldList.ccPos) 0
      else {
        val elemAccuracies = for {
          candElem <- candList.elemsRange
          candSize = rangeSize(candElem)
          (maxIntersection, goldSize) = goldList.elemsRange.map(g => (rangeIntersection(candElem, g), rangeSize(g))).max
          accuracy = maxIntersection.toDouble / Math.max(candSize.toDouble, goldSize.toDouble)
        } yield accuracy
        elemAccuracies.sum / candList.elemsRange.size.toDouble
      }
    }
    val (avgPrecision, avgRecall) = (calcAccuracy(candList, goldList), calcAccuracy(goldList, candList))
    Score(avgPrecision, avgRecall)
  }

  import ScoreImplicits._
  def addSentence(sentence: String, candidateLists: Seq[ListRange], goldLists: Seq[ListRange]): Seq[(Score, ListRange, ListRange)] = {
    candidateLists.map {
      case candList =>
        val (maxScore, goldList) = goldLists.map(l => (scoreList(candList, l), l)).maxBy(_._1)
        val maxElemLen = goldList.elemsRange.map(t => t._2 - t._1 + 1).max -
          goldList.elemsRange.map(t => t._2 - t._1 + 1).min
        scoreVector += ((sentence, candList, goldList, maxScore))
        if(!scoreSumByLength.contains(maxElemLen)) scoreSumByLength(maxElemLen) = (Score(0,0), 0)
        scoreSumByLength(maxElemLen) = (scoreSumByLength(maxElemLen)._1 + maxScore, scoreSumByLength(maxElemLen)._2 + 1)
        scoreSum = scoreSum + maxScore
        (maxScore, candList, goldList)
    }
  }

  def getScoreVector = scoreVector.toSeq
  def getAverageScore = scoreSum / scoreVector.size.toDouble
  def getAverageScoreByLength = scoreSumByLength.map{case (l,t) => (l, (t._1/t._2.toDouble, t._2))}
}
