package edu.iitd.nlp.ListExtraction

import org.allenai.common.LoggingWithUncaughtExceptions

import edu.berkeley.nlp.lm.NgramLanguageModel
import edu.berkeley.nlp.lm.collections.Iterators
import edu.berkeley.nlp.lm.io._

import scala.collection.JavaConversions

class LanguageModelWrapper {
  val vocabFile = "models/vocab_cs.gz"
  val binaryFile = "models/eng.blm.gz"
  val langModel = LmReaders.readGoogleLmBinary(binaryFile, vocabFile)

  def nGramProb(nGram: Seq[String]): Double = {
    langModel.getLogProb(JavaConversions.seqAsJavaList(nGram))
  }

  def computeAverageProb(listElem: Seq[String]): Double = {
    val listElemWithMarkers = Seq("<s>") ++ listElem ++ Seq("</s>")
    val windowLength = 2
    val windowProbs = listElemWithMarkers.sliding(windowLength).map(nGramProb).filter(!_.isNaN).toList
    if(windowProbs.isEmpty) 0
    else windowProbs.sum / windowProbs.size.toDouble
  }
}

object LanguageModelWrapperMain extends App with LoggingWithUncaughtExceptions{
  val languageModelWrapper = new LanguageModelWrapper
  val string = Seq("I","like","playing","cricket", ",")
  val prob = languageModelWrapper.computeAverageProb(string)
  logger.info(s"String: $string\tProbability: $prob")
}