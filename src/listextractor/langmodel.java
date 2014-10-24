package listextractor;

import java.io.*;
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

import edu.berkeley.nlp.lm.NgramLanguageModel;
import edu.berkeley.nlp.lm.collections.Iterators;
import edu.berkeley.nlp.lm.io.*;

public class langmodel{
	private Socket server;
	public langmodel(String serverName, String port) {
		server = new Socket(serverName, port);
	}
	public float readData() {
	    try {
		    InputStream socketStream = server.getInputStream();
		    ObjectInputStream objectInput = new ObjectInputStream(new GZIPInputStream(socketStream));
		    float a = (float) objectInput.readObject();
		    return a;
		} catch(Exception e) {
		    return null;
	    }
	}
	public void writeData(Arraylist<String> a) {
	    try {
	        OutputStream socketStream = socket.getOutputStream();
	        ObjectOutputStream objectOutput = new ObjectOutputStream(socketStream);
	        objectOutput.writeObject(a);
	        objectOutput.close();
	        socketStream.close();
	    } catch (Exception e) {
	        System.out.println(e.toString());
	    }
	}
	public float ngramprob(ArrayList<String> a){
		writeData(a);
		return readData();
	}
	double computeProb(ArrayList<String> words){
		double prob=0.; double cnt=0.;
		try {
			for(int i=0;i<words.size();i++){
				float cur = lm.getLogProb(words.subList(i,Math.min(words.size(),i+2)));
				if (Float.isNaN(cur)){
					cur = 0.4*lm.getLogProb(words.subList(i,i+1));
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
		if(words.size()>0) prob /= cnt;
		return prob;
	}

	NgramLanguageModel<String> readBinary(boolean isGoogleBinary, String vocabFile, String binaryFile) {
		NgramLanguageModel<String> lm = null;
		if (isGoogleBinary) {
			lm = LmReaders.readGoogleLmBinary(binaryFile, vocabFile);
		} else {
			lm = LmReaders.readLmBinary(binaryFile);
		}
		return lm;
	}

	public static void main(final String[] argv) throws FileNotFoundException, IOException {
		langmodel l =  new langmodel(argv[0],argv[1]);
		Scanner in = new Scanner(System.in);
		String text;
		while(in.hasNext()){
			text = in.nextLine();
			ArrayList<String> a = new ArrayList<String>(Arrays.asList(text.split("\\s*")));
			double prob = l.computeProb(a);
			System.err.print("Log probability of text is: ");
			System.out.println(prob);
		}
	}

}

