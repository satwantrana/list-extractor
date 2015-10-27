package edu.iitd.nlp.ListExtraction

import org.allenai.common.LoggingWithUncaughtExceptions

import edu.berkeley.nlp.lm.{ StupidBackoffLm, NgramLanguageModel }
import edu.berkeley.nlp.lm.io._

import scala.collection.JavaConversions

class LanguageModelWrapper extends LoggingWithUncaughtExceptions {
  val vocabFile = "models/vocab_cs.gz"
  val binaryFile = "models/eng.blm.gz"
  var langModel: Option[StupidBackoffLm[String]] = None

  def getNGramLogProb(nGram: Seq[String]): Double = {
    langModel.synchronized {
      if (langModel == null) {
        logger.info("Loading Language Model")
        langModel = Some(LmReaders.readGoogleLmBinary(binaryFile, vocabFile))
        logger.info("Loaded Language Model")
      }
    }
    val prob = langModel.get.getLogProb(JavaConversions.seqAsJavaList(nGram))
    if (prob.isNaN) 0.4 * getNGramLogProb(nGram.dropRight(1)) //Stupid Back                                                                                                                                                                                                      off
    else prob
  }

  def getAverageProb(listElem: Seq[String]): Double = {
    val listElemWithMarkers = Seq("<s>") ++ listElem ++ Seq("</s>")
    val windowLength = 3
    val windowProbs = listElemWithMarkers.sliding(windowLength).map(getNGramLogProb).filter(!_.isNaN).toList
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