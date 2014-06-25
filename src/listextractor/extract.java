package listextractor;

import java.io.*;
import java.util.*;
import java.lang.*;
import java.lang.Math;
import listextractor.deptree;

public class extract
{
	int[][] e; int[] vis; int vp; String[][] etype;
	deptree t;
	String[][][] s;

	public extract() throws Exception{
		t = new deptree();
	}

	public void process(String sent) throws Exception{
		s = t.get(sent);
	}
	public int[] sentsize(){
		int[] ret = new int[s.length];
		for(int i=0;i<s.length;i++) ret[i] = s[i].length;
		return ret;
	}

	public int[][][][] listmat(){
		int[][][][] ret = new int[s.length][][][];
		for(int i=0;i<s.length;i++){
			ret[i] = listextractor(s[i]);
		}
		return ret;
	}

	String[][][] lists(){
		int[][][][] list = listmat();
		String[][][] ret = new String[list.length][][];
		for(int idx=0;idx<list.length;idx++){
			ret[idx] = new String[list[idx].length][];
			for(int i=0;i<list[idx].length;i++){
				ret[idx][i] = new String[list[idx][i].length];
				for(int j=0;j<list[idx][i].length;j++){
					ret[idx][i][j] = "";
					for(int k=0;k<list[idx][i][j].length;k++){
						if(list[idx][i][j][k]==1) ret[idx][i][j]+=(s[idx][k][1]+" ");
					}
				}
			}	
		}
		return ret;
	}

	void listheads(int nd){
		if(vis[nd]==1) return;
		vis[nd]=1;
		for(int i=0;i<e.length;i++){
			if(e[i][nd]==2 || e[nd][i]==2) listheads(i);
		}
	}

	void listelems(int nd,int flag, int[] lh){
		if(vis[nd]==1) return;
		vis[nd]=1;
		String temp1,temp2,temp;
		for(int i=0;i<e.length;i++){
			if(e[i][nd] == 0 || lh[i] == 1) continue;
			if(flag==0 && i<nd && etype[i][nd].startsWith("nsubj") && vp == 1) continue;
			if(flag==1 && etype[i][nd].startsWith("nsubj")) vp = 0;
			listelems(i,2,lh);
		}
	}

	int[][][] listextractor(String[][] s){
		e = new int[s.length][s.length];
		etype = new String[s.length][s.length];
		for(int i=0;i<e.length;i++){
			for(int j=0;j<e[i].length;j++){
				e[i][j]=0;
				etype[i][j] = "";
			}
		}

		ArrayList<Integer> st = new ArrayList<Integer>();
		for(int i=0;i<s.length;i++){
			int k = Integer.parseInt(s[i][5])-1;
			if(k>=0) etype[i][k] = s[i][6];
			if(s[i][6].equals("cc")) st.add(i);
			if(s[i][6].equals("conj") || s[i][6].equals("cc")) e[i][k]=2;
			else if(k>=0) e[i][k]=1;
		}
		
		int[][][] list = new int[st.size()][][];
		int[] lh;
		for(int i=0;i<st.size();i++){
			vis = new int[s.length];
			Arrays.fill(vis,0);
			listheads(st.get(i));
			lh = new int[vis.length];
			System.arraycopy(vis,0,lh,0,vis.length);
			int cnt=0,tf=-1;
			for(int j=0;j<lh.length;j++){
			 	if(lh[j]==0 || s[j][6].equals("cc")) continue;
			 	cnt++;
			 	if(tf==-1) tf=j;
			}
			list[i] = new int[cnt][];
			if(cnt > 1)vp = 1;
			else vp = 0;
			cnt--;
			for(int j=lh.length-1;j>=0;j--){
				if(lh[j]==1){
					Arrays.fill(vis,0);
					if(s[j][6].equals("cc")) continue;
					else if(j==tf && s[j][3].startsWith("VB")) listelems(j,0,lh);
					else if(j!=tf && s[j][3].startsWith("VB")) listelems(j,1,lh);
					else listelems(j,2,lh);
					list[i][cnt] = new int[vis.length];
					System.arraycopy(vis,0,list[i][cnt],0,vis.length);
					int flag=0;
					for(int k=j;k<list[i][cnt].length;k++){
						if(flag==1) list[i][cnt][k] = 0;
						else if(list[i][cnt][k]==0)flag=1;
						else if(list[i][cnt][k]==1 && (k == (list[i][cnt].length-1) || list[i][cnt][k+1] == 0) && s[k][6].equals("punct")) list[i][cnt][k] = 0;
					}
					flag=0;
					for(int k=j;k>=0;k--){
						if(flag==1) list[i][cnt][k] = 0;
						else if(list[i][cnt][k]==0){
							flag=1;							
						}
					}
					cnt--;
				}
			}
		}
		return list;
	}
	
	public static void main(String[] args) throws Exception{
		extract e = new extract();
		String[][][] s;
		Scanner in = new Scanner(System.in);
		System.out.println("Good to Go!");
		while(in.hasNext()){
			e.process(in.nextLine());
			s = e.lists();
			for(int i=0;i<s.length;i++){
				System.out.println("Sentence #"+i+":");
				for(int j=0;j<s[i].length;j++){
					System.out.println("List #"+j+":");
					for(int k=0;k<s[i][j].length;k++){
						System.out.println(s[i][j][k]);
					}
					System.out.println();
				}
				System.out.println();
			}
		}

	}
}