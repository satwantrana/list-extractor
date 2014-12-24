package listextractor;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;
import java.lang.Math;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.lang.*;
import java.util.zip.*;

public class langmodel{
	Socket server; String serverName; int port;

	public langmodel(String _serverName, int _port) throws Exception {
		serverName = _serverName;
		port = _port;
	}
	public float readData() {
	    try {
		    InputStream socketStream = server.getInputStream();
		    ObjectInputStream objectInput = new ObjectInputStream(new GZIPInputStream(socketStream));
		    // System.out.println("Reading from client");
		    float a = (float) objectInput.readObject();
		    objectInput.close();
	        socketStream.close();
	        // System.out.println("Read from client: "+a);
		    return a;
		} catch(Exception e) {
		    e.printStackTrace();
		    return (float)0.;
	    }
	}
	public void writeData(ArrayList<String> a) {
	    try {
	        OutputStream socketStream = server.getOutputStream();
	        ObjectOutputStream objectOutput = new ObjectOutputStream(new GZIPOutputStream(socketStream));
	        // System.out.println("Writing from client");
	        objectOutput.writeObject(a);
	        objectOutput.close();
	        socketStream.close();
	        // System.out.println("Written from client: "+a);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	public float ngramprob(List<String> a){
		try{
			server = new Socket(serverName, port);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		writeData(new ArrayList<String>(a));
		try{
			server = new Socket(serverName, port);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return readData();
	}
	double computeProb(List<String> words){
		double prob=0.; double cnt=0.;
		try {
			for(int i=0;i<words.size();i++){
				float cur = ngramprob(words.subList(i,Math.min(words.size(),i+2)));
				if (Float.isNaN(cur)){
					cur = (float)0.4*ngramprob(words.subList(i,i+1));
					if(Float.isNaN(cur)) continue;
					//System.out.println(i+" "+words.get(i)+" : "+words.get(Math.min(i+1,words.size()-1)));
				}
				prob += cur;
				cnt += 1.;
			}
			// prob  =  Math.log(lm.scoreSentence(words));
		}
		catch (Exception e){
			//System.out.println("Exception");
			//for(int i=0;i<words.size();i++) System.out.print(i+":"+words.get(i)+" "+words.get(Math.min(i+1,words.size()-1)));
			//System.out.println();
			e.printStackTrace();
		}
		if(cnt>0) prob /= cnt;
		return prob;
	}

	public static void main(final String[] argv) throws Exception, FileNotFoundException, IOException {
		langmodel l =  new langmodel(argv[0],Integer.parseInt(argv[1]));
		Scanner in = new Scanner(System.in);
		String text;
		while(in.hasNext()){
			text = in.nextLine();
			List<String> a = Arrays.asList(text.split("\\s*"));
			System.out.println(a);
			double prob = l.computeProb(a);
			System.err.print("Log probability of text is: ");
			System.out.println(prob);
		}
	}

}


