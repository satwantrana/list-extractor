package listextractor;

import java.io.*;
import java.lang.*;
import java.util.*;
import java.lang.Math;

public class gold{
    public Integer[][][][] list(int[] lens) throws FileNotFoundException{
		Integer t,n,m,x,y;
		Integer[][][][] gold;
		Scanner in = new Scanner(new File("gold.txt"));
		t = in.nextInt();
		//		System.out.println("No of sentences: " + t);
		gold = new Integer[t][][][];
		for(Integer i=0;i<t;i++){


		    
		    //System.out.println(sentsz[i]); 
		    //System.out.println(i);
			n = in.nextInt();
			//	System.out.println(n);
			gold[i] = new Integer[n][][];
			for(Integer j=0;j<n;j++){
				m = in.nextInt();
				gold[i][j] = new Integer[m+1][lens[i]];
				Arrays.fill(gold[i][j][m],1);
				for(Integer k=0;k<m;k++){
					Arrays.fill(gold[i][j][k],0);
					x = in.nextInt();
					y = in.nextInt();
					//System.out.println(x + " " + y + " " + (i + 1)  + " " + sentsz[i]);
					for(Integer l=x;l<=y;l++){
						gold[i][j][k][l]=1;
						gold[i][j][m][l]=0;
					}
				}
			}
		}
		return gold;
	}
}