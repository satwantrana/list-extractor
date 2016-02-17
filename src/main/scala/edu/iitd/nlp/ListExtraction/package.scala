package edu.iitd.nlp.ListExtraction

import org.allenai.nlpstack

package object tokenize {
  val isPennTokenizer = false
  def defaultTokenizer: nlpstack.core.Tokenizer = if(isPennTokenizer) nlpstack.tokenize.PennTokenizer
    else nlpstack.tokenize.defaultTokenizer
}
