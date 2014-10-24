package listextractor;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.lang.Math;

import java.net.*;
import java.io.*;

public class similarity {
	private Socket server;
	public similarity(String servername, String port) throws Exception{
		server = new Socket(serverName, port);
	}
	public Double readData() {
	    try {
		    InputStream socketStream = server.getInputStream();
		    ObjectInputStream objectInput = new ObjectInputStream(new GZIPInputStream(socketStream));
		    Double a = (Double) objectInput.readObject();
		    return a;
		} catch(Exception e) {
		    return null;
	    }
	}
	public void writeData(String a, String b) {
	    try {
	        OutputStream socketStream = socket.getOutputStream();
	        ObjectOutputStream objectOutput = new ObjectOutputStream(socketStream);
	        objectOutput.writeObject(new String[] {a,b});
	        objectOutput.close();
	        socketStream.close();
	    } catch (Exception e) {
	        System.out.println(e.toString());
	    }
	}
	public Double wordsim(String a, String b, Integer port){
		writeData(new String[] {a,b});
		return readData();
	}
    public Double listsim(ArrayList<String> a, ArrayList<String> b){
    	Integer n = a.size(), m = b.size();
    	Double[][] dp = new Double[n+1][m+1];
    	Integer[][] cnt = new Integer[n+1][m+1];
		Integer[][][] path = new Integer[n+1][m+1][2];
		for(Integer i=0;i<=n;i++)
    		for(Integer j=0;j<=m;j++){
    			dp[i][j] = Double.NEGATIVE_INFINITY;
			cnt[i][j] = 0;
			path[i][j][0] = path[i][j][1] = 0;
		}
    	dp[0][0]=0.;
    	for(Integer i=1;i<=n;i++)
    		for(Integer j=1;j<=m;j++){
    			dp[i][j] = Math.max(dp[i][j],dp[i-1][j-1]+wordsim(a.get(i-1),b.get(j-1)));
    			dp[i][j] = Math.max(dp[i][j],dp[i][j-1]+wordsim(a.get(i-1),b.get(j-1)));
    			dp[i][j] = Math.max(dp[i][j],dp[i-1][j]+wordsim(a.get(i-1),b.get(j-1)));
				cnt[i][j] = 1000000000;
				if(dp[i][j] == dp[i-1][j-1]+wordsim(a.get(i-1),b.get(j-1)) && cnt[i][j] > cnt[i-1][j-1]+1){
					path[i][j][0] = i-1;
					path[i][j][1] = j-1;
	    			cnt[i][j] = cnt[i-1][j-1]+1;
				}
				if(dp[i][j] == dp[i][j-1]+wordsim(a.get(i-1),b.get(j-1)) && cnt[i][j] > cnt[i][j-1]+1){
					path[i][j][0] = i;
					path[i][j][1] = j-1;
					cnt[i][j] = cnt[i][j-1]+1;
				}
				if(dp[i][j] == dp[i-1][j]+wordsim(a.get(i-1),b.get(j-1)) && cnt[i][j] > cnt[i-1][j]+1){
					path[i][j][0] = i-1;
					path[i][j][1] = j;
					cnt[i][j] = cnt[i-1][j]+1;
				}
    		}
		Integer px = path[n][m][0], py = path[n][m][1];
		for(Integer i=0;i<a.size();i++) System.out.print(a.get(i)+" ");
		System.out.println();
		for(Integer i=0;i<b.size();i++) System.out.print(b.get(i)+" ");
		System.out.println();
		while(px>0 || py > 0){
			System.out.print(a.get(px-1)+":"+b.get(py-1)+"  ");
			Integer tpx = path[px][py][0], tpy = path[px][py][1];
			px = tpx;
			py = tpy;
		}
		System.out.println();
		if(cnt[n][m]>0) dp[n][m]/=cnt[n][m];
			System.out.println(Math.log(dp[n][m])+"\n");
		return Math.log(dp[n][m]);
    }

    public static void main(String[] args) throws Exception{
        similarity s = new similarity(args[0], Integer.parseInt(args[1]));
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
