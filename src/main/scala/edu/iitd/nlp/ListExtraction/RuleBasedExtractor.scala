package edu.iitd.nlp.ListExtraction

import org.allenai.common.LoggingWithUncaughtExceptions
import org.allenai.nlpstack.core.PostaggedToken
import org.allenai.nlpstack.core.parse.graph.{DependencyGraph, DependencyNode}
import org.allenai.nlpstack.parse.{defaultDependencyParser => parser}
import org.allenai.nlpstack.postag.{ defaultPostagger => postagger }
import org.allenai.nlpstack.tokenize.{ defaultTokenizer => tokenizer }

import scala.collection.mutable

case class ListRange(ccPos: Int, elemsRange: Seq[(Int, Int)])
case class List(cc: String, elems: Seq[String])

class RuleBasedExtractor {
  def extractListRange(sentence: String): (Seq[PostaggedToken], DependencyGraph, Seq[ListRange]) = {
    val (tokens, parse) = parser.dependencyGraph(tokenizer, postagger)(sentence)

    val adjMap = parse.edges.map(e => (e.source,(e.dest,e.label))).groupBy(_._1)
      .map{case (n,s) => (n,s.map(_._2))}

    val lists = mutable.ArrayBuffer[ListRange]()

    def dfs(node: DependencyNode): (Boolean,Int,Int) = {
      val res = if(adjMap.getOrElse(node,Seq()).isEmpty){
        Seq(((tokens(node.id).isCoordinatingConjunction, node.id,node.id),""))
      }
      else{
        val dfsRes = adjMap(node).map{case (n,l) => (dfs(n),l)}.toSeq
        val ranges = dfsRes ++ Seq(((tokens(node.id).isCoordinatingConjunction, node.id, node.id),""))
        ranges
      }
      val coordinatingConjuncts = res.filter(x => x._1._1).map(x => x._1._2)
      val ccId = if(coordinatingConjuncts.nonEmpty) coordinatingConjuncts.last else -1
      val filteredRes = res.filter(x => !x._1._1 && x._2 == "conj").map(x => (x._1._2,x._1._3))
      if (ccId != -1 && filteredRes.nonEmpty){
        lists += ListRange(ccId, filteredRes)
      }
      val curRange = (tokens(node.id).isCoordinatingConjunction, res.map(x => x._1._2).min, res.map(x => x._1._3).max)
      curRange
    }
    dfs(parse.root.get)
    (tokens, parse, lists)
  }

  def extractLists(tokens: Seq[PostaggedToken], lists: Seq[ListRange]): Seq[List] = {
    lists.map {
      case ListRange(ccId, elems) => List(
        tokens(ccId).string, elems.map(e => tokens.slice(e._1, e._2+1).map(_.string).mkString(" "))
      )
    }
  }
}

object RuleBasedExtractorMain extends LoggingWithUncaughtExceptions with App{
  val extractor = new RuleBasedExtractor
  val sent = "I like playing hockey, cricket and football."
  logger.info(s"Sentence: $sent")
  val (tokens, parse, listRanges) = extractor.extractListRange(sent)
  val lists = extractor.extractLists(tokens, listRanges)
  logger.info(s"Parse Tree: $parse\nLists: $lists")
}
