package edu.iitd.nlp.ListExtraction

import org.allenai.nlpstack.core.PostaggedToken
import org.allenai.nlpstack.core.parse.graph.DependencyGraph

case class ListRange(ccPos: Int, elemsRange: Seq[(Int, Int)])
case class List(cc: String, elems: Seq[String])

trait ListExtractor {
  def extractListRange(sentence: String): (Seq[PostaggedToken], DependencyGraph, Seq[ListRange])
  def extractLists(tokens: Seq[PostaggedToken], lists: Seq[ListRange]): Seq[List]
}
