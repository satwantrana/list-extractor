package edu.iitd.nlp.ListExtraction

import org.allenai.common.LoggingWithUncaughtExceptions
import org.allenai.nlpstack.core.PostaggedToken
import org.allenai.nlpstack.core.parse.graph.DependencyGraph

import scala.collection.mutable

case class ListRange(ccPos: Int, elemsRange: mutable.ArrayBuffer[(Int, Int)], var conf: Double)
case class List(cc: String, elems: Seq[String], var conf: Double)

trait ListExtractor extends LoggingWithUncaughtExceptions{
  def extractListRange(sentence: String): (Seq[PostaggedToken], DependencyGraph, Seq[ListRange])
  def extractLists(tokens: Seq[PostaggedToken], lists: Seq[ListRange]): Seq[List] = {
    lists.map {
      case ListRange(ccId, elems, conf) => List(
        tokens(ccId).string, elems.map(e => tokens.slice(e._1, e._2 + 1).map(_.string).mkString(" ")), conf
      )
    }
  }
}
