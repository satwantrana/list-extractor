package listextractor;

import listextractor.deptree;
import listextractor.extract;
import listextractor.langmodel;
import listextractor.similarity;

import java.io.*;
import java.util.*;
import java.lang.*;
import java.lang.Math;
import org.javatuples.*;

public class gextract{
	extract e;
	langmodel l;
	similarity s;
	String[][][] dep;
	Integer[][][][] list;
	public gextract(deptree t, String lmVocabFile, String lmBinaryFile, String similarityFile) throws Exception{
		e = new extract(t);
		s = new similarity(similarityFile);
		l = new langmodel(true,lmVocabFile,lmBinaryFile);
	}
	public Integer[] sentsize(){
		return e.sentsize();
	}
	Double problist(Integer[][] clist, String[][] cdep, Double p, Double q){
		ArrayList< ArrayList< String> > a = new ArrayList< ArrayList <String> > ();
		ArrayList<String> b;
		Double sprob=0., lprob = 0.;
		for(Integer i=0;i<clist.length;i++){
			b = new ArrayList<String>();
		//	for(Integer j=0;j<clist[0][0]; j++){
		//		b.add(cdep[j][1]);
		//	}
			for(Integer j=clist[i][0]; j<=clist[i][1]; j++){
				b.add(cdep[j][1]);
			}
		//	for(Integer j=clist[clist.length-1][1]+1; j<cdep.length; j++){
		//		b.add(cdep[j][1]);
		//	}
			a.add(b);
			if(i>0) sprob += s.listsim(a.get(i),a.get(0));
			if(i==clist.length-1){
				for(int j=1;j<clist.length-1;j++) sprob += s.listsim(a.get(i),a.get(j));
			}
			//lprob += l.computeProb(b);
		}
		if(clist.length>0) lprob /= clist.length;
		if(clist.length>1) sprob /= 2.*clist.length-3.;
		//System.out.println(sprob+" "+lprob);
		return p*sprob+q*lprob;
	}
	Pair<Integer, Integer> bestpair(Integer[][] clist, String[][] cdep, Double p, Double q){
		Integer le = clist[0][0], re = clist[clist.length-1][1];
		Pair<Integer,Integer> pair = Pair.with(le,re);
		Double prob = Double.NEGATIVE_INFINITY;
		for(Integer i=0;i<=le;i++){
			for(Integer j=re;j<cdep.length;j++){
				clist[0][0] = i; clist[clist.length-1][1]=j;
				Double curprob = problist(clist,cdep,p,q);
				if(prob < curprob){
					prob = curprob;
					pair  = Pair.with(i,j);
				}
				System.out.println("Pair: "+i+":"+j+" "+curprob+" "+prob);
			}
		}
		System.out.println(pair);
		return pair;
	}
	public void process(String sent) throws Exception{
		e.process(sent);
		dep = e.getdeptree();
		list = e.lists();
	}
	public Integer[][][][] lists(Double p, Double q){
		for(Integer i=0;i<list.length;i++){
			for(Integer j=0;j<list[i].length;j++){
				Pair<Integer,Integer> pair = bestpair(list[i][j], dep[i], p, q);
				list[i][j][0][0] = pair.getValue0();
				list[i][j][list[i][j].length-1][1] = pair.getValue1();
			}
		}
		return list;
	}
	public String[][][] getlists(Double p, Double q){
		String[][][] ret = new String[list.length][][];
		list = lists(p,q);
		for(Integer idx=0;idx<list.length;idx++){
			ret[idx] = new String[list[idx].length][];
			for(Integer i=0;i<list[idx].length;i++){
				ret[idx][i] = new String[list[idx][i].length];
				for(Integer j=0;j<list[idx][i].length;j++){
					ret[idx][i][j] = "";
					for(Integer k=list[idx][i][j][0];k<=list[idx][i][j][1];k++){
						ret[idx][i][j]+=(dep[idx][k][1]+" ");
					}
				}
			}	
		}
		return ret;
	}
	public void print(Double p, Double q){
		String[][][] slist = getlists(p,q);
		for(Integer i=0;i<slist.length;i++){
			System.out.println("Sent #"+i+":");
			for(Integer j=0;j<slist[i].length;j++){
				System.out.println("List #"+j+":");
				for(Integer k=0;k<slist[i][j].length;k++){
					System.out.println(slist[i][j][k]);
				}
			}
		}
	}
	public static void main(String[] args) throws Exception{
		deptree tree = new deptree();
		gextract e = new gextract(tree,args[0],args[1],args[2]);
		Scanner in = new Scanner(System.in);
		String text = "";
		while(in.hasNext()){
			text = in.nextLine();
			e.process(text);
			e.print(Double.parseDouble(args[3]),Double.parseDouble(args[4]));
		}
	}
}
