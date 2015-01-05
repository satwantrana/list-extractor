package listextractor;

import java.io.*;
import java.lang.*;
import java.util.*;
import java.lang.Math;
import listextractor.deptree;
import listextractor.gextract;
import listextractor.gold;
import listextractor.hungarian;

public class score{
	Integer[][][] cand,gold;	Integer len;
	double[] sc;	public double pre,pcnt,rec,rcnt;
	public score(Integer[][][] c,Integer[][][] g,Integer l){
		cand = c;
		gold = g;
		len = l;
		pre = rec = 0;
		pcnt = rcnt = 0;
	}
	double lscore(Integer x,Integer y){
		if(x>=cand.length || y>=gold.length) return 0;
		Integer n = Math.max(cand[x].length,gold[y].length);
		double[][] cost = new double[n][n];
		boolean[] pc = new boolean[len];
		boolean[] pg = new boolean[len]; 
		for(Integer i=0;i<n;i++){
			for(Integer j=0;j<n;j++){
				cost[i][j]=0;
				if(i>=cand[x].length || j>=gold[y].length) continue;
				Arrays.fill(pc,false);
				Arrays.fill(pg,false);
				for(Integer l=0;l<len;l++)
					if(cand[x][i][l]==1) pc[l] = true;
				for(Integer l=0;l<len;l++)
					if(gold[y][j][l]==1) pg[l] = true;
				double tot=0;
				for(Integer k=0;k<len;k++){
					if(pg[k]&&pc[k]) cost[i][j]++;
					if(pg[k]||pc[k]) tot++;
				}
				cost[i][j]/=tot;
			}
		}
		hungarian h = new hungarian(cost,n);
		h.solve();
		pre += h.match()/(1.*cand[x].length); rec += h.ymatch()/(1.*gold[y].length);
		pcnt++; rcnt++;
		return h.match()/n;
	}
	double[] solve(){
		Integer n = Math.max(cand.length,gold.length);
		double[][] cost = new double[n][n];
		boolean[] pc = new boolean[len];
		boolean[] pg = new boolean[len];
		for(Integer i=0;i<n;i++){
			for(Integer j=0;j<n;j++){
				cost[i][j]=0;
				if(i>=cand.length || j>=gold.length) continue;
				Arrays.fill(pc,false);
				Arrays.fill(pg,false);
				for(Integer k=0;k<cand[i].length-1;k++)
					for(Integer l=0;l<len;l++)
						if(cand[i][k][l]==1) pc[l] = true;
				for(Integer k=0;k<gold[j].length-1;k++)
					for(Integer l=0;l<len;l++)
						if(gold[j][k][l]==1) pg[l] = true;
				double tot=0;
				for(Integer k=0;k<len;k++){
					if(pg[k]&&pc[k]) cost[i][j]++;
					if(pg[k]||pc[k]) tot++;
				}
				cost[i][j]/=tot;
			}
		}
		hungarian h = new hungarian(cost,n);
		h.solve();
		Integer[] match = h.assign();
		sc = new double[cand.length];
		for(Integer idx=0;idx<cand.length;idx++){
			sc[idx] = lscore(idx,match[idx]);
		}
		return sc;
	}

    
	public static void main(String[] args) throws FileNotFoundException,Exception{
		
		deptree d = new deptree();
		//extract e = new extract(d);
		gextract e = new gextract(d,"localhost",Integer.parseInt(args[0]),"localhost",Integer.parseInt(args[1]));
		BufferedReader in = new BufferedReader(new FileReader(new File("inp.txt"))); 
		ArrayList<String> inp = new ArrayList<String>();
		String line; 
		while((line = in.readLine()) != null)inp.add(line);
		int[] lens = new int[inp.size()];
		for (int i=0;i<inp.size();i++){
		    lens[i] = inp.get(i).split(" ").length;
		}
		//System.out.println(inp.get(0)+"\n"+inp.get(inp.size()-1)+"\n");
		Integer[][][][] gold,cand;
		cand = new Integer[lens.length][][][];
		Integer[][] temp; Integer[] sentsz = new Integer[lens.length]; Integer t,n,m,x,y;
		//System.out.println("Num of sentences: "+inp.size()+" "+lens.length+" "+sentsz.length);

	        for(int i=0;i<cand.length;i++){
		    e.process(inp.get(i));
		    //cand[i]  = e.lists()[0];
		    cand[i] = e.lists(Double.parseDouble(args[2]),Double.parseDouble(args[3]))[0]; 
		    sentsz[i] = e.sentsize()[0];
		}

		t = cand.length;
		for(Integer i=0;i<t;i++){
			n = cand[i].length;
			for(Integer j=0;j<n;j++){
				m = cand[i][j].length;
				temp = new Integer[m][sentsz[i]];
				for(Integer k=0;k<m;k++){
					Arrays.fill(temp[k],0);
					for(Integer l=cand[i][j][k][0];l<=cand[i][j][k][1];l++){
						temp[k][l] = 1;
					}
				}
				cand[i][j] = new Integer[m+1][sentsz[i]];
				for(Integer k=0;k<m;k++){
					for(Integer l=0;l<cand[i][j][k].length;l++){
						cand[i][j][k][l] = temp[k][l];
					}
				}
				Arrays.fill(cand[i][j][m],1);
				for(Integer k=0;k<m;k++){
					for(Integer l=0;l<cand[i][j][k].length;l++){
						if(cand[i][j][k][l]==1) cand[i][j][m][l]=0;
					}
				}
			}	
		}

		gold g = new gold();
		
		gold = g.list(lens);

		score s;

		double[] sentsc; double avg=0,cnt=0,cntc=0,cntg=0,pre=0,rec=0;
		Integer scnt = 0;
		//System.out.println(cand.length+" "+gold.length);
		for(Integer i=0;i<Math.min(sentsz.length, lens.length);i++){
			System.out.println("Sentence #"+i+":");
			System.out.println("Candidate Lists: "+cand[i].length+"\tGold Lists: "+gold[i].length);
			if(lens[i] == sentsz[i]){
			    s = new score(cand[i],gold[i],sentsz[i]);
			    sentsc = s.solve();
			    for(Integer j=0;j<sentsc.length;j++){
				System.out.print("List #"+j+": "+sentsc[j]+"\t");
				avg += sentsc[j];
			    }
			    cnt += cand[i].length;
			    pre += s.pre; rec += s.rec;
			    cntc += s.pcnt; cntg += s.rcnt;
			    scnt++;
			    System.out.println();
			}
		}
		avg /= cnt;
		pre /= cntc;
		rec /= cntg;
		System.out.println("Average Score: "+avg+"\tPrecision: "+pre+"\tRecall: "+rec+"\tSentences: "+scnt);
	}
}
