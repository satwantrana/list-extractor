package listextractor;

import edu.berkeley.nlp.lm.NgramLanguageModel;
import edu.berkeley.nlp.lm.collections.Iterators;
import edu.berkeley.nlp.lm.io.*;

import java.net.*;
import java.io.*;
import java.util.zip.*;
import java.util.*;

public class berkeleylm extends Thread{
	NgramLanguageModel<String> lm;
	private ServerSocket serverSocket;

	public berkeleylm(String vocabFile, String binaryFile,int port) throws Exception{
		lm = LmReaders.readGoogleLmBinary(binaryFile, vocabFile);
		serverSocket = new ServerSocket(port);
      	//serverSocket.setSoTimeout(10000);
	}

	public float ngramprob(ArrayList<String> ngram){
		return lm.getLogProb(ngram);
	}

	public ArrayList<String> readData(Socket server) {
	    try {
		    InputStream socketStream = server.getInputStream();
		    ObjectInputStream objectInput = new ObjectInputStream(new GZIPInputStream(socketStream));
		    // System.out.println("Reading from server");
		    ArrayList<String> a = (ArrayList<String>) objectInput.readObject();
		    objectInput.close();
	        socketStream.close();
		    // System.out.println("Read from server: "+a);
		    return a;
		} catch(Exception e) {
		    e.printStackTrace();
		    return null;
	    }
	}

	public void writeData(Socket server, float d) {
	    try {
	        OutputStream socketStream = server.getOutputStream();
	        ObjectOutputStream objectOutput = new ObjectOutputStream(new GZIPOutputStream(socketStream));
	        // System.out.println("Writing from server");
	        objectOutput.writeObject(d);
	        objectOutput.close();
	        socketStream.close();
	        // System.out.println("Written from server: "+d);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	public void run(){
    	while(true){
         	try{
	            // System.out.println("Waiting for client on port " +serverSocket.getLocalPort() + "...");
	            Socket server = serverSocket.accept();
	            // System.out.println("Just connected to "+ server.getRemoteSocketAddress());
	           	ArrayList<String> a = readData(server);
	           	server.close();
	           	server = serverSocket.accept();
	            writeData(server, ngramprob(a));
	            server.close();
         	}
         	catch(SocketTimeoutException s){
            	System.out.println("Socket timed out!");
            	break;
         	}
         	catch(IOException e){
            	e.printStackTrace();
            	break;
        	}
        	catch(Exception e){
        		e.printStackTrace();
        	}
    	}
   	}
   	public static void main(String [] args) throws Exception{
    	int port = Integer.parseInt(args[2]);
      	try{
         	Thread t = new berkeleylm(args[0],args[1],port);
         	t.start();
      	}
      	catch(IOException e){
         	e.printStackTrace();
      	}
   	}
}

