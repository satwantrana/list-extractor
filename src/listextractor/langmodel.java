package listextractor;

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

import edu.berkeley.nlp.lm.NgramLanguageModel;
import edu.berkeley.nlp.lm.collections.Iterators;
import edu.berkeley.nlp.lm.io.*;

public class langmodel{

	NgramLanguageModel<String> lm;
	
	public langmodel(boolean isGoogleBinary, String vocabFile, String binaryFile) {
		lm = readBinary(isGoogleBinary,vocabFile,binaryFile);
	}

	double computeProb(ArrayList<String> words){
		return lm.getLogProb(words);
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
		
		int i = 0;
		boolean isGoogleBinary = false;
		if (argv[i].equals("-g")) {
			isGoogleBinary = true;
			i++;
		}
		String vocabFile = null;
		if (isGoogleBinary) {
			vocabFile = argv[i++];
		}
		String binaryFile = argv[i++];
		
		langmodel l =  new langmodel(isGoogleBinary,vocabFile,binaryFile);
		
		ArrayList<String> a = new ArrayList<String>();
		a.add("My");
		a.add("Name");
		a.add("is");
		a.add("Jon");

		double prob = l.computeProb(a);
		System.err.print("Log probability of text is: ");
		System.out.println(prob);
	}

}
