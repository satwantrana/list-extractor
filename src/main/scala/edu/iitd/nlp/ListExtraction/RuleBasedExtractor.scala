package edu.iitd.nlp.ListExtraction

import org.allenai.common.LoggingWithUncaughtExceptions
import org.allenai.nlpstack.core.PostaggedToken
import org.allenai.nlpstack.core.parse.graph.{ DependencyGraph, DependencyNode }
import org.allenai.nlpstack.parse.{ defaultDependencyParser => parser }
import org.allenai.nlpstack.postag.{ defaultPostagger => postagger }
import org.allenai.nlpstack.tokenize.{ defaultTokenizer => tokenizer }

import scala.collection.mutable
import scala.collection.JavaConversions._

class RuleBasedExtractor extends ListExtractor {
  def extractListRangeParsed(tokens: Seq[PostaggedToken], parse: DependencyGraph): Seq[ListRange] = {
    val adjMap = parse.edges.foldLeft(Map[DependencyNode, Set[(DependencyNode, String)]]()) {
      case (m, e) => m.updated(e.source, m.getOrElse(e.source, Set()) + ((e.dest, e.label)))
        .updated(e.dest, m.getOrElse(e.dest, Set()) + ((e.source, e.label)))
    }
    val childMap = parse.edges.foldLeft(Map[DependencyNode, Set[(DependencyNode, String)]]()) {
      case (m, e) => m.updated(e.source, m.getOrElse(e.source, Set()) + ((e.dest, e.label)))
    }

    def dfs(node: DependencyNode): (Int, Int) = {
      val children = childMap.getOrElse(node, Set()).filter(x => x._2 != "cc" && x._2 != "conj")

      val res = if (children.isEmpty) Set((node.id, node.id))
      else children.map(c => dfs(c._1)) + ((node.id, node.id))

      val range = (res.map(_._1).min, res.map(_._2).max)
      range
    }

    val pruneFromEnd = Set(",", ".")

    val lists = adjMap.filter {
      case (n, s) =>
        val labels = s.map(_._2)
        labels.contains("cc") && labels.contains("conj")
    }.map {
      case (n, s) =>
        val cc = s.filter(_._2 == "cc").last._1
        val elems = s.filter(_._2 == "conj").map(_._1) + n
        val elemsRange = elems.map(dfs).map {
          case (s, e) =>
            if (s < e && e < tokens.size && pruneFromEnd.contains(tokens(e).string)) (s, e - 1)
            else (s, e)
        }.toSeq.sorted
        ListRange(cc.id, mutable.ArrayBuffer(elemsRange: _*), 1.0)
    }.toSeq
    lists
  }

  def extractListRange(sentence: String): (Seq[PostaggedToken], DependencyGraph, Seq[ListRange]) = {
    val (tokens, parse) = parser.dependencyGraph(tokenizer, postagger)(sentence)
    (tokens, parse, extractListRangeParsed(tokens, parse))
  }
}

object RuleBasedExtractorMain extends LoggingWithUncaughtExceptions with App {
  val sent = "I like playing hockey, cricket and football."
  logger.info(s"Sentence: $sent")

  val extractor = new RuleBasedExtractor
  val (tokens, parse, listRanges) = extractor.extractListRange(sent)
  val lists = extractor.extractLists(tokens, listRanges)
  logger.info(s"Tokens: $tokens\nParse Tree: $parse\nLists: $lists\n")

  val scorer = new MaxMatchScorer
  val goldListRanges = Seq(ListRange(6, mutable.ArrayBuffer((3, 3), (5, 5), (7, 7)), 1.0))
  scorer.addSentence(sent, listRanges, goldListRanges)
  logger.info(s"Cand: $listRanges\nGold: $goldListRanges\nScore: ${scorer.getAverageScore}")
}
