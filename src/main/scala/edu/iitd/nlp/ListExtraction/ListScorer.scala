package edu.iitd.nlp.ListExtraction

import scala.collection.mutable

case class Score(precision: Double, recall: Double){
  def +(other: Score) = {
    Score(precision + other.precision, recall + other.recall)
  }

  def /(num: Double) = {
    Score(precision/num, recall/num)
  }
}

trait ListScorer {
  def scoreList(candidateList: ListRange, goldList: ListRange): Score
  def addSentence(sentence: String, candidateLists: Seq[ListRange], goldLists: Seq[ListRange]): Unit
  def getScoreVector: Seq[(String, ListRange, Score)]
  def getAverageScore: Score
}

class MaxMatchScorer extends ListScorer{
  val scoreVector = mutable.ArrayBuffer.empty[(String, ListRange, Score)]
  var scoreSum = Score(0,0)

  def rangeIntersection(a: (Int,Int), b: (Int,Int)): Int = {
    Math.max(0, Math.min(a._2,b._2) - Math.max(a._1,b._1))
  }

  def scoreList(candList: ListRange, goldList: ListRange): Score = {
    if(candList.ccPos != goldList.ccPos) Score(0,0)
    else{
      val elemScores = for {
        candElem <- candList.elemsRange
        maxIntersection = goldList.elemsRange.map(rangeIntersection(candElem,_)).max
        precision = maxIntersection.toDouble/candList.elemsRange.size.toDouble
        recall = maxIntersection.toDouble/candList.elemsRange.size.toDouble
        score = Score(precision,recall)
      } yield score
      val avgPrecision = elemScores.map(_.precision).sum / elemScores.size.toDouble
      val avgRecall = elemScores.map(_.recall).sum / elemScores.size.toDouble
      Score(avgPrecision,avgRecall)
    }
  }

  def orderByPrecision(a: Score, b:Score): Boolean = {
    a.precision < b.precision
  }

  def addSentence(sentence: String, candidateLists: Seq[ListRange], goldLists: Seq[ListRange]): Unit = {
    candidateLists.foreach{
      case candList =>
        val maxScore = goldLists.map(scoreList(candList,_)).max
        scoreVector +=  ((sentence, candList, maxScore))
        scoreSum = scoreSum + maxScore
    }
  }

  def getScoreVector = scoreVector.toSeq

  def getAverageScore = scoreSum/scoreVector.size.toDouble
}
