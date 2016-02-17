package edu.iitd.nlp.ListExtraction

import org.allenai.common.LoggingWithUncaughtExceptions
import org.allenai.nlpstack.postag.{ defaultPostagger => postagger }
import tokenize.{ defaultTokenizer => tokenizer }
import org.allenai.nlpstack.chunk.{ defaultChunker => chunker }

import scala.collection.mutable
import scala.io.Source

class PhraseSimilarity extends LoggingWithUncaughtExceptions {
  val wordVectorWrapper = new WordVectorWrapper
  var weightVector = FeatureVector.bagOfWordsSimilarity()
  
  def getPhraseSimilarityVector(p: String, q:String): FeatureVector = {
    val tp = chunker.chunk(tokenizer, postagger)(p)
    val tq = chunker.chunk(tokenizer, postagger)(q)
    val res = wordVectorWrapper.getFeatureDPPhraseSimilarity(tp, tq, weightVector, Params(0, 0))
//    logger.info(s"Similarity vector for $p and $q is $res")
    res
  }

  def getPhraseSimilarity(p: String, q: String): Double = {
    wordVectorWrapper.sigmoid(weightVector * getPhraseSimilarityVector(p, q))
  }

  def evaluate(question: String, options: Set[String]): String = {
    val res = options.map(o => (getPhraseSimilarity(question, o), o)).maxBy(_._1)._2
//    logger.info(s"Answer for $question $options is $res")
    res
  }

  def trimModelsToSentences(file: Option[String], inpSents: Option[Seq[String]]) = {
    val sentences = mutable.Set[String]()
    logger.info("Aggregating Sentences")
    if (file.isDefined) {
      val data = Source.fromFile(file.get).getLines()
      while(data.hasNext) {
        val sent = data.next()
        sentences.add(sent)
      }
    }
    if (inpSents.isDefined) {
      inpSents.get.foreach(sentences.add(_))
    }
    wordVectorWrapper.initialised.set(false)
    wordVectorWrapper.sentences = sentences.toSeq
  }
}

object PhraseSimilarityMain extends App with LoggingWithUncaughtExceptions {
  val phraseSimilarity = new PhraseSimilarity
  val sentence = "double star | binary | double | star | dual | lumen | neutralism | keratoplasty"
  phraseSimilarity.trimModelsToSentences(None, Some(Seq(sentence)))
  logger.info(s"Similarity of 'double star' and 'binary' is ${phraseSimilarity.getPhraseSimilarity("double star", "binary")}")
  logger.info(s"Similarity of 'double star' and 'dual' is ${phraseSimilarity.getPhraseSimilarity("double star", "dual")}")
}

