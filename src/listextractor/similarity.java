package listextractor;

import org.deeplearning4j.util.SerializationUtils;
import org.deeplearning4j.word2vec.Word2Vec;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.lang.Math;

public class similarity {
	Word2Vec vec;
	
	public similarity(String file){
		vec = SerializationUtils.readObject(new File(file));
	}

	public Double wordsim(String a, String b){
		return Math.log(vec.similarity(a,b));
	}

    public Double listsim(ArrayList<String> a, ArrayList<String> b){
    	Integer n = a.size(), m = b.size();
    	Double[][] dp = new Double[n+1][m+1];
    	for(Integer i=0;i<=n;i++)
    		for(Integer j=0;j<=m;j++)
    			dp[i][j] = Double.NEGATIVE_INFINITY;
    	dp[0][0]=0.;
    	for(Integer i=1;i<=n;i++)
    		for(Integer j=1;j<=m;j++){
    			dp[i][j] = Math.max(dp[i][j],dp[i-1][j-1]+wordsim(a.get(i-1),b.get(i-1)));
    			dp[i][j] = Math.max(dp[i][j],dp[i][j-1]);
    			dp[i][j] = Math.max(dp[i][j],dp[i-1][j]);
    		}
    	return dp[n][m]/Math.max(n,m);
    }

    public static void main(String[] args) {
        similarity s = new similarity(args[0]);
        System.out.println("Similarity of " + s.wordsim("China","Taiwan"));

        ArrayList<String> a,b;
        a = new ArrayList<String>();
        b = new ArrayList<String>();

        a.add("Irvine");
        a.add(",");
        a.add("CA");

        b.add("Seattle");
        b.add(",");
        b.add("WA");

        System.out.println(s.listsim(a,b));
    }

}