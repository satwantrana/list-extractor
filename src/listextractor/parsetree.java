package listextractor;

import java.io.StringReader;
import java.util.*;
import org.javatuples.*;

import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.parser.shiftreduce.ShiftReduceParser;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class parsetree {
  MaxentTagger tagger; 
  //ShiftReduceParser model;
   LexicalizedParser model;

  public parsetree(String modelPath, String taggerPath) throws Exception{
      tagger = new MaxentTagger(taggerPath);
      //model = ShiftReduceParser.loadModel(modelPath);
       model = LexicalizedParser.loadModel(modelPath);
  }

  void dfs(TreeGraphNode node,String bar){
    // System.out.println(node.nodeString()+" "+node.value()+" "+node.label().value()+" "+node.label().toString()+" "+node.isLeaf());
    TreeGraphNode[] child = node.children();
    System.out.println(bar+"(\n"+bar+node.nodeString());
    for(Integer i=0;i<child.length;i++) dfs(child[i],bar+"\t");
    System.out.println(bar+")");
  }

  Pair<List<Tree>,List<List<TaggedWord> > > get(String text){
      List<Tree> trees = new ArrayList<Tree>();
      List<List<TaggedWord> > tokens = new ArrayList<List<TaggedWord> >();
      DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(text));
      TreebankLanguagePack tlp = new PennTreebankLanguagePack();
      GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
      for (List<HasWord> sentence : tokenizer) {
        List<TaggedWord> tagged = tagger.tagSentence(sentence);
        tokens.add(tagged);
        // Tree tree = model.apply(sentence);
        Tree tree = model.apply(tagged);
        trees.add(tree);
        tree.pennPrint();
        GrammaticalStructure gs = gsf.newGrammaticalStructure(tree);
        TypedDependency[] tdl = gs.typedDependencies().toArray(new TypedDependency[0]);
        TreeGraphNode root = new TreeGraphNode();
        for(Integer i=0;i<tdl.length;i++){
          if(tdl[i].reln().toString().equals("conj")) System.out.println(tdl[i].reln().toString()+" "+tdl[i].gov().nodeString()+" "+tdl[i].dep().nodeString()+" "+tdl[i].gov().index()+" "+tdl[i].dep().index());
          //if(tdl[i].reln().toString().equals("root")) root = tdl[i].dep();
          // Collection lis = tdl.get(i).reln().
        }
        //System.out.println(root.index());
        dfs(root,"");
      }
      return Pair.with(trees,tokens);
  }

  public static void main(String[] args) throws Exception{
    String modelPath = args[1];
    String taggerPath = args[0];

    String text = "It also houses paintings from artists like Van der Helst, Vermeer, Frans Hals, Ferdinand Bol, Albert Cuyp, Jacob van Ruisdael and Paulus Potter.";
    parsetree p = new parsetree(modelPath,taggerPath);
    p.get(text);
  }
}