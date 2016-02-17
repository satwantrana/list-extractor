package edu.iitd.nlp.ListExtraction

import java.util.concurrent.atomic.{ AtomicInteger, AtomicBoolean }

import org.allenai.common.LoggingWithUncaughtExceptions

import edu.berkeley.nlp.lm.{ StupidBackoffLm, NgramLanguageModel }
import edu.berkeley.nlp.lm.io._
import tokenize.{ defaultTokenizer => tokenizer }

import scala.collection.JavaConversions
import scala.collection.mutable

class LanguageModelWrapper extends LoggingWithUncaughtExceptions {
  val vocabFile = "models/vocab_cs.gz"
  val binaryFile = "models/eng.blm.gz"
  var langModel = mutable.Map[Seq[String], Double]()
  val initialised = new AtomicBoolean(false)
  var sentences: Seq[String] = Seq()
  val nGramWindowLength = 3

  def readModel(): Unit = {
    logger.info(s"Creating word filter from ${sentences.size} sentences")
    val tokenisedSentences = sentences.map(s => Seq("<s>") ++ tokenizer.tokenize(s).map(_.string) ++ Seq("</s>"))
    logger.info("Loading Language Model")
    val lm = LmReaders.readGoogleLmBinary(binaryFile, vocabFile)
    val nGramCount = new AtomicInteger(0)
    def createAllNGrams(tokens: Seq[String], currentNGram: Seq[String], idx: Int, pos: Int): Unit = {
      nGramCount.addAndGet(1)
      langModel(currentNGram) = lm.getLogProb(JavaConversions.seqAsJavaList(currentNGram))
      if (idx < nGramWindowLength) for (i <- 0 until tokens.size) {
        val word = tokens(i)
        createAllNGrams(tokens, currentNGram ++ Seq(word), idx + 1, i)
      }
    }
    tokenisedSentences.foreach(createAllNGrams(_, Seq(), 0, -1))
    logger.info(s"Total Possible nGrams: ${nGramCount.get}")
    initialised.set(true)
    logger.info("Loaded Language Model")
  }

  def getNGramLogProb(nGram: Seq[String]): Double = {
    initialised.synchronized {
      if (!initialised.get) readModel()
    }
    val prob = langModel(nGram)
    if (prob.isNaN) 0.4 * getNGramLogProb(nGram.dropRight(1)) //Stupid Back                                                                                                                                                                                                      off
    else prob
  }

  def getAverageProb(listElem: Seq[String]): Double = {
    val listElemWithMarkers = Seq("<s>") ++ listElem ++ Seq("</s>")
    val windowProbs = listElemWithMarkers.sliding(nGramWindowLength).map(getNGramLogProb).filter(!_.isNaN).toList
    if (windowProbs.isEmpty) 0
    else Math.exp(windowProbs.sum / windowProbs.size.toDouble)
  }
}

object LanguageModelWrapperMain extends App with LoggingWithUncaughtExceptions {
  val languageModelWrapper = new LanguageModelWrapper
  val string = Seq("I", "like", "playing", "cricket", ",")
  val prob = languageModelWrapper.getAverageProb(string)
  logger.info(s"String: $string\tProbability: $prob")
}