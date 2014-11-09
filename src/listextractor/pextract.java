package listextractor;

import java.io.*;
import java.util.*;
import java.lang.*;
import java.lang.Math;
import org.javatuples.*;
import listextractor.parsetree;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.shiftreduce.ShiftReduceParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.Tree;

public class pextract
{
	parsetree t;
	List<Tree> trees;
	List<List<TaggedWord> > tokens;
	Integer[][] cclist;
	Integer[][][][] list;
	Integer tot;
	Map<Integer,Integer> index;

	public pextract(parsetree tree) throws Exception{
		t = tree;
	}

	public void process(String sent) throws Exception{
		Pair<List<Tree>,List<List<TaggedWord> > > p = t.get(sent);
		trees = p.getValue0();
		tokens = p.getValue1();
		cclist = new Integer[trees.size()][];
		list = new Integer[trees.size()][][][];
		for(Integer i=0;i<trees.size();i++) listextractor(i);
	}

	public Integer[] sentsize(){
		Integer[] ret = new Integer[trees.size()];
		for(Integer i=0;i<ret.length;i++) ret[i] = tokens.get(i).size();
		return ret;
	}

	public Integer[][][][] lists(){
		return list;
	}

	public Integer[][] conjunctions(){
		return cclist;
	}

	// Integer index(String label){
	// 	String[] temp = label.split("-");
	// 	return Integer.parseInt(temp[temp.length-1])-1;
	// }

	public void listheads(Integer idx, Tree nd){
		Tree[] child = nd.children();
		Integer flag=-1;
		for(Integer i=0;i<child.length;i++){
			listheads(idx,child[i]);
			if(child[i].value().equals("CC") && !child[i].isLeaf()) flag=i;
		}
		if(flag>=0){
			Integer cnt=0,mn=flag,mx=flag,comma=1;
			for(Integer i=flag-1;i>=0;i--){
				if(child[i].value().equals(",") || child[i].value().equals(".")){
					comma=1;
					continue;
				}
				Integer[] temp = listelems(child[i],idx);
				if(tokens.get(idx).get(temp[1]).tag().equals(",")) comma=1;
				if(comma==0) break;
				cnt++;
				mn=i;
				comma=0;
			}
			for(Integer i=flag+1;i<child.length;i++){
				if(child[i].value().equals(",") || child[i].value().equals(".")) continue;
				cnt++;
				mx=i;
				break;
			}
			list[idx][tot] = new Integer[cnt][2];
			cnt=0;
			cclist[idx][tot] = index.get(child[flag].children()[0].nodeNumber(trees.get(idx)));
			System.out.println(idx+" "+tot+" "+cclist[idx][tot]);
			for(Integer i=mn;i<=mx;i++){
				if(i==flag) continue;
				if(child[i].value().equals(",") || child[i].value().equals(".")) continue;
				list[idx][tot][cnt] = listelems(child[i],idx);
				child[i].pennPrint();
				System.out.println(list[idx][tot][cnt][0]+" "+list[idx][tot][cnt][1]);
				String temp = tokens.get(idx).get(list[idx][tot][cnt][1]).tag();
				if(temp.equals(",")||temp.equals(".")) list[idx][tot][cnt][1]--;
				cnt++;
			}
			tot++;
		}
	}

	public Integer[] listelems(Tree nd,Integer idx){
		Integer[] ret = new Integer[2];
		ret[0]=1000000000; ret[1]=0;
		if(nd.isLeaf()) ret[0] = ret[1] = index.get(nd.nodeNumber(trees.get(idx)));
		Tree[] child = nd.children();
		for(Integer i=0;i<child.length;i++){
			Integer[] temp = listelems(child[i],idx);
			ret[0] = Math.min(ret[0],temp[0]);
			ret[1] = Math.max(ret[1],temp[1]);
		}
		// System.out.println(nd.label().toString()+" "+ret[0]+" "+ret[1]);
		return ret;
	}

	void calcIndex(Tree node,Integer idx){
		if(node.isLeaf()){
			index.put(node.nodeNumber(trees.get(idx)),tot++);
			// System.out.println(node.label().toString()+":"+index.get(node.nodeNumber(trees.get(idx)))+"\t");
		}
		else index.put(node.nodeNumber(trees.get(idx)),-1);
		Tree[] child = node.children();
		for(Integer i=0;i<child.length;i++) calcIndex(child[i],idx);
	}

	public void listextractor(Integer idx){
		tot=0;
		index = new HashMap<Integer,Integer>();
		calcIndex(trees.get(idx),idx);
		// System.out.println();
		tot=0;
		for(Integer i=0;i<tokens.get(idx).size();i++){
			if(tokens.get(idx).get(i).tag().equals("CC")) tot++;
		}
		list[idx] = new Integer[tot][][];
		cclist[idx] = new Integer[tot];
		tot=0;
		// System.out.println(trees.get(idx));
		listheads(idx, trees.get(idx));
	}

	String word(String raw){
		Integer i=0;
		for(i=raw.length()-1;i>=0;i--){
			if(raw.charAt(i)=='/') break;
		}
		return raw.substring(0,i);
	}

	public String[][][] getlists(){
		// Integer[][][][] list = lists();
		String[][][] ret = new String[list.length][][];
		for(Integer idx=0;idx<list.length;idx++){
			ret[idx] = new String[list[idx].length][];
			for(Integer i=0;i<list[idx].length;i++){
				if(list[idx][i]==null) continue;
				ret[idx][i] = new String[list[idx][i].length];
				for(Integer j=0;j<list[idx][i].length;j++){
					ret[idx][i][j] = "";
					for(Integer k=list[idx][i][j][0];k<=list[idx][i][j][1];k++){
						ret[idx][i][j]+=(word(tokens.get(idx).get(k).toString())+" ");
					}
					// System.out.println(list[idx][i][j][0]+" "+list[idx][i][j][1]+":\t"+ret[idx][i][j]);
					System.out.println(ret[idx][i][j]);
				}
				System.out.println();
			}	
			System.out.println();
		}
		return ret;
	}
	// void print(){
	// 	String[][][] sdep; Integer[][][][] lmat; Integer[][] conj;
	// 	sdep = getdeptree();
	// 	lmat = lists();
	// 	conj = conjunctions();
	// 	System.out.println(lmat.length); // #Sentences
	// 	for(Integer i=0;i<lmat.length;i++){
	// 		System.out.println(sdep[i].length); // #Tokens
	// 		for(Integer j=0;j<sdep[i].length;j++){
	// 			System.out.println(sdep[i][j][1]); // Token
	// 		}
	// 		System.out.println(lmat[i].length); // #Conjunctions
	// 		for(Integer j=0;j<lmat[i].length;j++){
	// 			System.out.println(conj[i][j]); // Conj token number - 0 indexed
	// 			System.out.println(lmat[i][j].length); // #	Conjuncts
	// 			for(Integer k=0;k<lmat[i][j].length;k++){
	// 				System.out.println(lmat[i][j][k][0]+" "+lmat[i][j][k][1]); // Conjunct range
	// 			}
	// 			//System.out.println();
	// 		}
	// 		//System.out.println();
	// 	}
	// }
	public static void main(String[] args) throws Exception{
		parsetree tree = new parsetree(args[1],args[0]);
		pextract e = new pextract(tree);
		Scanner in = new Scanner(System.in);
		String text = "";
		while(in.hasNext()){
			text = in.nextLine();
			e.process(text);
			e.getlists();
		}
	}
}
