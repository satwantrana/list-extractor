package listextractor;

import java.io.*;
import java.util.*;
import java.lang.*;
import com.clearnlp.component.AbstractComponent;
import com.clearnlp.dependency.DEPTree;
import com.clearnlp.nlp.NLPGetter;
import com.clearnlp.nlp.NLPMode;
import com.clearnlp.reader.AbstractReader;
import com.clearnlp.segmentation.AbstractSegmenter;
import com.clearnlp.tokenization.AbstractTokenizer;
import com.clearnlp.util.UTInput;
import com.clearnlp.util.UTOutput;

public class deptree
{
	final String language = AbstractReader.LANG_EN, modelType = "general-en";
	
	AbstractTokenizer tokenizer;
	AbstractComponent tagger, parser, identifier, classifier, labeler;
	
	public deptree() throws Exception{
		tokenizer  = NLPGetter.getTokenizer(language);
		tagger     = NLPGetter.getComponent(modelType, language, NLPMode.MODE_POS);
		parser     = NLPGetter.getComponent(modelType, language, NLPMode.MODE_DEP);
		identifier = NLPGetter.getComponent(modelType, language, NLPMode.MODE_PRED);
		classifier = NLPGetter.getComponent(modelType, language, NLPMode.MODE_ROLE);
		labeler    = NLPGetter.getComponent(modelType, language, NLPMode.MODE_SRL);
	}

	public String[][][] get(String sent) throws Exception{
		AbstractComponent[] components = {tagger, parser, identifier, classifier, labeler};
		if(sent == null) return new String[0][0][0];
		BufferedReader reader = new BufferedReader(new StringReader(sent));
		return process(tokenizer, components, reader);
	}

	String[][][] process(AbstractTokenizer tokenizer, AbstractComponent[] components, BufferedReader reader) throws Exception{
		AbstractSegmenter segmenter = NLPGetter.getSegmenter(language, tokenizer);
		DEPTree tree;
		List< List<String> > sentences = segmenter.getSentences(reader);
		String[][][] s = new String[sentences.size()][][]; int idx = 0;
		
		for (List<String> tokens : sentences)
		{
			tree = NLPGetter.toDEPTree(tokens);
			for (AbstractComponent component : components)
				component.process(tree);
			String[] r = tree.toStringDEP().split("\n");
			s[idx] = new String[r.length][];
			for(int i=0;i<r.length;i++){
				s[idx][i] = r[i].split("\t");
			}
			idx++;
		}
		return s;
	}

	public static void main(String args[]) throws Exception{
		deptree t = new deptree();
		String[][][] s;
		Scanner in = new Scanner(System.in);
		System.out.println("Good to Go!");
		while(in.hasNext()){
			s = t.get(in.nextLine());
			for(int i=0;i<s.length;i++){
				for(int j=0;j<s[i].length;j++){
					for(int k=0;k<s[i][j].length;k++){
						System.out.print(s[i][j][k]+"\t");
					}
					System.out.println();
				}
				System.out.println();
			}
		}
	}
}