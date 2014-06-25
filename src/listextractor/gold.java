package listextractor;

import java.io.*;
import java.lang.*;
import java.util.*;
import java.lang.Math;

public class gold{
	public int[][][][] list(int[] sentsz) throws FileNotFoundException{
		int t,n,m,x,y;
		int[][][][] gold;
		Scanner in = new Scanner(new File("gold.txt"));
		t = in.nextInt();
		gold = new int[t][][][];
		for(int i=0;i<t;i++){
			n = in.nextInt();
			gold[i] = new int[n][][];
			for(int j=0;j<n;j++){
				m = in.nextInt();
				gold[i][j] = new int[m+1][sentsz[i]];
				Arrays.fill(gold[i][j][m],1);
				for(int k=0;k<m;k++){
					Arrays.fill(gold[i][j][k],0);
					x = in.nextInt();
					y = in.nextInt();
					for(int l=x;l<=y;l++){
						gold[i][j][k][l]=1;
						gold[i][j][m][l]=0;
					}
				}
			}
		}
		return gold;
	}
}