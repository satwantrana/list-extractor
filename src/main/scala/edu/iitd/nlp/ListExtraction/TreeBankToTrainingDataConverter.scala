package edu.iitd.nlp.ListExtraction

import java.io.{ File, PrintWriter }

import edu.stanford.nlp.trees.{ Tree, EnglishGrammaticalStructure }
import org.allenai.common.LoggingWithUncaughtExceptions
import org.allenai.nlpstack.core.PostaggedToken
import org.allenai.nlpstack.parse.{ defaultDependencyParser => parser }
import org.allenai.nlpstack.core.parse.graph.{ DependencyGraph, DependencyNode }
import org.allenai.nlpstack.postag.{ defaultPostagger => postagger }
import org.allenai.nlpstack.tokenize.{ defaultTokenizer => tokenizer }

import scala.collection.JavaConversions._
import scala.io.Source

object TreeBankToTrainingDataConverter extends LoggingWithUncaughtExceptions {
  val ruleBasedExtractor = new RuleBasedExtractor
  val DEBUG = false
  def extractLists(tokens: Seq[PostaggedToken], parseString: String): Seq[ListRange] = {
    val dependencies = new EnglishGrammaticalStructure(Tree.valueOf(parseString)).typedDependencies().toList.mkString("; ")
    val parse = DependencyGraph.singlelineStringFormat.read(dependencies)
    if (DEBUG) logger.info(s"${tokens} $parse")
    ruleBasedExtractor.extractListRangeParsed(tokens, parse)
  }
  def processPennTreeBank(pennTreeBankFile: String, outputFile: String): Unit = {
    val reader = Source.fromFile(pennTreeBankFile).getLines()
    val writer = new PrintWriter(new File(outputFile))
    var (cnt, linecnt, output) = (0, 0, "")

    val numSentences = reader.next().toInt
    for (i <- 0 until numSentences) {
      val tokenCount = reader.next().toInt
      val sent = reader.next()

      val tokens = sent.split(" ")
      val postags = reader.next().split(" ")
      val offsets = tokens.scanLeft(0)((c, t) => c + t.length + 1)
      val postaggedTokens = tokens.zip(postags).zip(offsets).map { case ((t, p), o) => PostaggedToken(p, t, o) }

      if (DEBUG) logger.info(s"${offsets.toList} ${postaggedTokens.toList}")

      val parseSize = reader.next().toInt
      val parseString = (for {
        j <- 0 until parseSize
        nextLine = reader.next()
      } yield nextLine).mkString("\n")

      val lists = extractLists(postaggedTokens, parseString)
      if (lists.nonEmpty) {
        cnt += 1
        output += sent + "\n"
        output += s"${lists.size}\n"
        require(sent.count(_ == '\n') == 0)
        linecnt += 2
      }
      for (list <- lists) {
        output += s"${list.ccPos} ${list.elemsRange.size}\n"
        linecnt += 1
        for (elem <- list.elemsRange) {
          output += s"${elem._2 - elem._1 + 1}\n"
//          val range = (for {
//            j <- elem._1 to elem._2
//          } yield j).mkString(" ")
          val range = s"${elem._1} ${elem._2}"
          output += s"$range\n"
          linecnt += 2
        }
      }
    }
    logger.info(s"$linecnt ${output.count(_ == '\n')}")
    require(linecnt == output.count(_ == '\n'))
    writer.write(s"$cnt\n$output")
    writer.close()
  }
}

object TreeBankToTrainingDataConverterMain extends App with LoggingWithUncaughtExceptions {
  TreeBankToTrainingDataConverter.processPennTreeBank("data/penn_treebank", "data/penn_treebank_dataset")
}
