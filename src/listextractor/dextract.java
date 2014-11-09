package listextractor;

import java.io.*;
import java.util.*;
import java.lang.*;
import java.lang.Math;
import org.javatuples.*;
import listextractor.deptree;

public class dextract
{
	deptree t;
	String[][][] dep;
	ArrayList< ArrayList<Pair<Integer,String> > > adj;
	Integer vp;
	Integer[] vis;
	Integer[][] cclist;
	Integer[][][][] list;

	public dextract(deptree tree) throws Exception{
		t = tree;
	}

	public void process(String sent) throws Exception{
		dep = t.get(sent);
		cclist = new Integer[dep.length][];
		list = new Integer[dep.length][][][];
		for(Integer i=0;i<dep.length;i++) listextractor(i);
	}
	
	public String[][][] getdeptree(){
		return dep;
	}

	public Integer[] sentsize(){
		Integer[] ret = new Integer[dep.length];
		for(Integer i=0;i<dep.length;i++) ret[i] = dep[i].length;
		return ret;
	}

	public Integer[][][][] lists(){
		return list;
	}

	public Integer[][] conjunctions(){
		return cclist;
	}

	void listheads(Integer nd){
		if(vis[nd]==1) return;
		vis[nd]=1;
		for(Integer i=0;i<adj.get(nd).size();i++){
			if(adj.get(nd).get(i).getValue1().equals("conj") || adj.get(nd).get(i).getValue1().equals("cc")) listheads(adj.get(nd).get(i).getValue0());
		}
	}
	void listelems(Integer nd, Boolean flag){
		if(vis[nd]==1) return;
		vis[nd]=1;
		for(Integer i=0;i<adj.get(nd).size();i++){
			if(adj.get(nd).get(i).getValue1().equals("conj") || adj.get(nd).get(i).getValue1().equals("cc")) continue;
			//if(!flag && adj.get(nd).get(i).getValue0()<nd && adj.get(nd).get(i).getValue1().startsWith("nsubj") && vp == 1) continue;
			//if(flag && adj.get(nd).get(i).getValue1().startsWith("nsubj")) vp = 0;
			listelems(adj.get(nd).get(i).getValue0(),flag);
		}
	}
	void listextractor(Integer idx){
		ArrayList<Integer> st = new ArrayList<Integer> ();
		adj = new ArrayList<ArrayList<Pair<Integer,String> > > ();
		for(Integer i=0;i<dep[idx].length;i++){
			adj.add(new ArrayList<Pair<Integer,String> >());
		}
		for(Integer i=0;i<dep[idx].length;i++){
			Integer k = Integer.parseInt(dep[idx][i][5])-1;
			if(k>=0) adj.get(k).add(Pair.with(i,dep[idx][i][6]));
			if(k>=0 && (dep[idx][i][6].equals("conj") || dep[idx][i][6].equals("cc"))) adj.get(i).add(Pair.with(k,dep[idx][i][6]));
			if(dep[idx][i][6].equals("cc")){
				st.add(i);
			}
		}
		cclist[idx] = new Integer[st.size()];
		list[idx] = new Integer[st.size()][][];
		vis = new Integer[dep[idx].length];
		Integer[] lh = new Integer[vis.length];
		for(Integer i=0;i<st.size();i++){
			cclist[idx][i] = st.get(i);
			Arrays.fill(vis,0);
			listheads(st.get(i));
			vis[st.get(i)]=0;
			System.arraycopy(vis,0,lh,0,vis.length);
			Integer cnt=0,it=0; vp=0;
			for(Integer j=0;j<lh.length;j++){
				cnt += lh[j];
				if(lh[j]==1 && dep[idx][j][3].startsWith("VB")) vp=1;
			}
			it=cnt-1;
			list[idx][i] = new Integer[cnt][2];
			for(Integer j=lh.length-1;j>=0;j--){
				if(lh[j]==0) continue;
				Arrays.fill(vis,0);
				listelems(j,it>0);
				Integer mn=vis.length+1,mx=-1;
				for(Integer k=j;k<vis.length;k++){
					if(vis[k]==0) break;
					mx = Math.max(mx,k);
				}
				for(Integer k=j;k>=0;k--){
					if(vis[k]==0) break;
					mn = Math.min(mn,k);
				}
				// if(it==0) mn=j;
				// if(it==cnt-1) mx=j;
				if(dep[idx][mx][1].equals(",") || dep[idx][mx][1].equals(".")) mx--;
				list[idx][i][it][0]=mn;
				list[idx][i][it][1]=mx;
				//list[idx][i][it][2]=j;
				it--;
			}
		}
	}
	public String[][][] getlists(){
		String[][][] ret = new String[list.length][][];
		for(Integer idx=0;idx<list.length;idx++){
			ret[idx] = new String[list[idx].length][];
			for(Integer i=0;i<list[idx].length;i++){
				ret[idx][i] = new String[list[idx][i].length];
				for(Integer j=0;j<list[idx][i].length;j++){
					ret[idx][i][j] = "";
					for(Integer k=list[idx][i][j][0];k<=list[idx][i][j][1];k++){
						ret[idx][i][j]+=(dep[idx][k][1]+" ");
					}
					System.out.println(ret[idx][i][j]);
				}
				System.out.println();
			}
			System.out.println();	
		}
		return ret;
	}
	void print(){
		String[][][] sdep; Integer[][][][] lmat; Integer[][] conj;
		sdep = getdeptree();
		lmat = lists();
		conj = conjunctions();
		System.out.println(lmat.length); // #Sentences
		for(Integer i=0;i<lmat.length;i++){
			System.out.println(sdep[i].length); // #Tokens
			for(Integer j=0;j<sdep[i].length;j++){
				System.out.println(sdep[i][j][1]); // Token
			}
			System.out.println(lmat[i].length); // #Conjunctions
			for(Integer j=0;j<lmat[i].length;j++){
				System.out.println(conj[i][j]); // Conj token number - 0 indexed
				System.out.println(lmat[i][j].length); // #	Conjuncts
				for(Integer k=0;k<lmat[i][j].length;k++){
					System.out.println(lmat[i][j][k][0]+" "+lmat[i][j][k][1]); // Conjunct range
				}
				//System.out.println();
			}
			//System.out.println();
		}
	}
	public static void main(String[] args) throws Exception{
		deptree tree = new deptree();
		dextract e = new dextract(tree);
		Scanner in = new Scanner(System.in);
		String text = "";
		while(in.hasNext()){
			text = in.nextLine();
			e.process(text);
			e.getlists();
			// e.print();
		}
	}
}
