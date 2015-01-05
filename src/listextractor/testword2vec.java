package listextractor;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.lang.Math;

import java.net.*;
import java.io.*;
import java.util.zip.*;
import java.util.*; 

public class testword2vec {
	Socket server; String serverName; int port;
	public testword2vec(String _serverName, int _port) throws Exception{
		serverName = _serverName;
		port = _port;
	}
	public Double readData() {
	    try {
		    InputStream socketStream = server.getInputStream();
		    ObjectInputStream objectInput = new ObjectInputStream(new GZIPInputStream(socketStream));
		    // System.out.println("Reading from client");
		    Double a = (Double) objectInput.readObject();
		    objectInput.close();
	        socketStream.close();
	        // System.out.println("Read from client: "+a);
		    return a;
		} catch(Exception e) {
		    e.printStackTrace();
		    return null;
	    }
	}
	public void writeData(String a, String b) {
	    try {
	        OutputStream socketStream = server.getOutputStream();
	        ObjectOutputStream objectOutput = new ObjectOutputStream(new GZIPOutputStream(socketStream));
	        ArrayList<String> arr = new ArrayList<String>();
	        arr.add(a); arr.add(b);
	       	// System.out.println("Writing from client "+ arr);
	        objectOutput.writeObject(arr);
	        objectOutput.close();
	        socketStream.close();
	        // System.out.println("Written from client: "+a+" "+b);

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	public Double wordsim(String a, String b){
		try{
			server = new Socket(serverName, port);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		writeData(a,b);
		try{
			server = new Socket(serverName, port);
		}
		catch(Exception e){
			e.printStackTrace();
		}
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
	Scanner input = new Scanner(System.in);
	while(true){
		System.out.println("enter two words"); 
		
		String a = input.next(); 
		
		if( a.charAt(0) == '#') break;
		String b = input.next(); 
		System.out.println("received");
		System.out.println(s.wordsim(a, b));  
	}
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
