package listextractor;

import org.deeplearning4j.util.SerializationUtils;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.models.word2vec.loader.Word2VecLoader;
import java.net.*;
import java.io.*;
import java.util.zip.*;
import java.util.*;

public class word2vec extends Thread{
	Word2Vec vec;
	private ServerSocket serverSocket;

	public word2vec(String file,int port) throws Exception{
	    // vec = Word2VecLoader.loadGoogleModel(file);
	    vec = Word2VecLoader.loadGoogleModel(file,false);
	    serverSocket = new ServerSocket(port);
	}
	public Double wordsim(String a, String b){
		//return 1.23;
		//System.out.println(Math.log(vec.similarity(a,b)));
		return Math.log(vec.similarity(a,b));
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
	public void writeData(Socket server, Double d) {
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
	           	//server.close();
	           	// System.out.println(a);
	           	//for(Integer i=0;i<a.length;i++) System.out.println(i+"->"+a[i]+" ");
	           	server = serverSocket.accept();
	            writeData(server, wordsim(a.get(0),a.get(1)));
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
    	}
   	}
   	public static void main(String [] args) throws Exception{
    	int port = Integer.parseInt(args[1]);
      	try{
         	Thread t = new word2vec(args[0],port);
		System.err.println("Word2Vec Server Ready.");
		t.start();
      	}
      	catch(IOException e){
         	e.printStackTrace();
      	}
   	}
}

