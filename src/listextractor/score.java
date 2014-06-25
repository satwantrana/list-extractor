package listextractor;

import java.io.*;
import java.lang.*;
import java.util.*;
import java.lang.Math;
import listextractor.extract;
import listextractor.gold;
import listextractor.hungarian;

public class score{
	int[][][] cand,gold;	int len;
	double[] sc;	public double pre,pcnt,rec,rcnt;
	public score(int[][][] c,int[][][] g,int l){
		cand = c;
		gold = g;
		len = l;
		pre = rec = 0;
		pcnt = rcnt = 0;
	}
	double lscore(int x,int y){
		if(x>=cand.length || y>=gold.length) return 0;
		int n = Math.max(cand[x].length,gold[y].length);
		double[][] cost = new double[n][n];
		boolean[] pc = new boolean[len];
		boolean[] pg = new boolean[len]; 
		for(int i=0;i<n;i++){
			for(int j=0;j<n;j++){
				cost[i][j]=0;
				if(i>=cand[x].length || j>=gold[y].length) continue;
				Arrays.fill(pc,false);
				Arrays.fill(pg,false);
				for(int l=0;l<len;l++)
					if(cand[x][i][l]==1) pc[l] = true;
				for(int l=0;l<len;l++)
					if(gold[y][j][l]==1) pg[l] = true;
				double tot=0;
				for(int k=0;k<len;k++){
					if(pg[k]&&pc[k]) cost[i][j]++;
					if(pg[k]||pc[k]) tot++;
				}
				cost[i][j]/=tot;
			}
		}
		hungarian h = new hungarian(cost,n);
		h.solve();
		pre += h.match()/h.mcount(); rec += h.ymatch()/h.ymcount();
		pcnt++; rcnt++;
		return h.match()/n;
	}
	double[] solve(){
		int n = Math.max(cand.length,gold.length);
		double[][] cost = new double[n][n];
		boolean[] pc = new boolean[len];
		boolean[] pg = new boolean[len];
		for(int i=0;i<n;i++){
			for(int j=0;j<n;j++){
				cost[i][j]=0;
				if(i>=cand.length || j>=gold.length) continue;
				Arrays.fill(pc,false);
				Arrays.fill(pg,false);
				for(int k=0;k<cand[i].length-1;k++)
					for(int l=0;l<len;l++)
						if(cand[i][k][l]==1) pc[l] = true;
				for(int k=0;k<gold[j].length-1;k++)
					for(int l=0;l<len;l++)
						if(gold[j][k][l]==1) pg[l] = true;
				double tot=0;
				for(int k=0;k<len;k++){
					if(pg[k]&&pc[k]) cost[i][j]++;
					if(pg[k]||pc[k]) tot++;
				}
				cost[i][j]/=tot;
			}
		}
		hungarian h = new hungarian(cost,n);
		h.solve();
		int[] match = h.assign();
		sc = new double[cand.length];
		for(int idx=0;idx<cand.length;idx++){
			sc[idx] = lscore(idx,match[idx]);
		}
		return sc;
	}

	public static void main(String[] args) throws FileNotFoundException,Exception{
		
		extract e = new extract();
		Scanner in = new Scanner(new File("inp.txt"));
		String inp = "";
		while(in.hasNext()) inp += in.nextLine() + "\n";
		e.process(inp);
		int[][][][] gold,cand = e.listmat(); int[][] temp; int[] sentsz = e.sentsize(); int t,n,m,x,y;
		
		t = cand.length;
		
		for(int i=0;i<t;i++){
			n = cand[i].length;
			for(int j=0;j<n;j++){
				m = cand[i][j].length;
				temp = new int[m][sentsz[i]];
				for(int k=0;k<m;k++){
					for(int l=0;l<cand[i][j][k].length;l++){
						temp[k][l] = cand[i][j][k][l];
					}
				}
				cand[i][j] = new int[m+1][sentsz[i]];
				for(int k=0;k<m;k++){
					for(int l=0;l<cand[i][j][k].length;l++){
						cand[i][j][k][l] = temp[k][l];
					}
				}
				Arrays.fill(cand[i][j][m],1);
				for(int k=0;k<m;k++){
					for(int l=0;l<cand[i][j][k].length;l++){
						if(cand[i][j][k][l]==1) cand[i][j][m][l]=0;
					}
				}
			}	
		}

		gold g = new gold();
		gold = g.list(sentsz);

		score s;

		double[] sentsc; double avg=0,cnt=0,cntc=0,cntg=0,pre=0,rec=0;

		for(int i=0;i<29;i++){
			System.out.println("Sentence #"+i+":");
			System.out.println("Candidate Lists: "+cand[i].length+"\tGold Lists: "+gold[i].length);
			s = new score(cand[i],gold[i],sentsz[i]);
			sentsc = s.solve();
			for(int j=0;j<sentsc.length;j++){
				System.out.print("List #"+j+": "+sentsc[j]+"\t");
				avg += sentsc[j];
			}
			cnt += cand[i].length;
			pre += s.pre; rec += s.rec;
			cntc += s.pcnt; cntg += s.rcnt;
			System.out.println();
		}
		avg /= cnt;
		pre /= cntc;
		rec /= cntg;
		System.out.println("Average Score: "+avg+"\tPrecision: "+pre+"\tRecall: "+rec);
	}
}