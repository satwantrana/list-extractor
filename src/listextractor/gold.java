package listextractor;

import java.io.*;
import java.lang.*;
import java.util.*;
import java.lang.Math;

public class gold{
	Integer[][][][] gold;
	Integer[] sentsz;
	public gold() throws FileNotFoundException{
		Integer t,n,m,x,y,ccpos; String s;
		Scanner in = new Scanner(new File("data.txt"));
		t = in.nextInt(); //Number of sentences
		// System.out.println(t);
		gold = new Integer[t][][][];
		sentsz = new Integer[t];
		for(Integer i=0;i<t;i++){
			s = in.nextLine();
			s = in.nextLine(); //Sentence
			// System.out.println(s);
			String[] temps = s.split(" ");
			sentsz[i] = temps.length; //Number of tokens
			n = in.nextInt(); //Number of lists
			// System.out.println(n);
			gold[i] = new Integer[n][][];
			for(Integer j=0;j<n;j++){
				ccpos = in.nextInt(); //CC Posn
				m = in.nextInt(); //Number of conjuncts
				gold[i][j] = new Integer[m+1][sentsz[i]];
				Arrays.fill(gold[i][j][m],1);
				for(Integer k=0;k<m;k++){
					Arrays.fill(gold[i][j][k],0);
					x = in.nextInt(); //Number of tokens in conjunct 
					for(Integer l=0;l<x;l++){
						y = in.nextInt(); //Token id
						gold[i][j][k][y]=1;
						gold[i][j][m][y]=0;
					}
				}
			}
		}
	}

	public Integer[] sentsize(){
		return sentsz;
	}

	public Integer[][][][] list(Integer[] sentsz){
		return gold;
	}

	public static void main(String[] args) throws FileNotFoundException{
		gold g = new gold();
	}
}