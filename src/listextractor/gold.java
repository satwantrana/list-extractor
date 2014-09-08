package listextractor;

import java.io.*;
import java.lang.*;
import java.util.*;
import java.lang.Math;

public class gold{
	public Integer[][][][] list(Integer[] sentsz) throws FileNotFoundException{
		Integer t,n,m,x,y;
		Integer[][][][] gold;
		Scanner in = new Scanner(new File("gold.txt"));
		t = in.nextInt();
		gold = new Integer[t][][][];
		for(Integer i=0;i<t;i++){
			n = in.nextInt();
			gold[i] = new Integer[n][][];
			for(Integer j=0;j<n;j++){
				m = in.nextInt();
				gold[i][j] = new Integer[m+1][sentsz[i]];
				Arrays.fill(gold[i][j][m],1);
				for(Integer k=0;k<m;k++){
					Arrays.fill(gold[i][j][k],0);
					x = in.nextInt();
					y = in.nextInt();
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