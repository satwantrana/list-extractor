package listextractor;

import edu.berkeley.nlp.lm.NgramLanguageModel;
import edu.berkeley.nlp.lm.collections.Iterators;
import edu.berkeley.nlp.lm.io.*;

import java.net.*;
import java.io.*;

public class berkeleylm{
	NgramLanguageModel<String> lm;
	private ServerSocket serverSocket;

	public berkeleylm(boolean isGoogleBinary, String vocabFile, String binaryFile, String port) throws Exception{
		lm = readBinary(isGoogleBinary,vocabFile,binaryFile);
		serverSocket = new ServerSocket(port);
      	serverSocket.setSoTimeout(10000);
	}

	public float ngramprob(ArrayList<String> ngram){
		return lm.getLogProb(ngram);
	}

	public ArrayList<String> readData(Socket server) {
	    try {
		    InputStream socketStream = server.getInputStream();
		    ObjectInputStream objectInput = new ObjectInputStream(new GZIPInputStream(socketStream));
		    Arraylist<String> a = (ArrayList<String>) objectInput.readObject();
		    return a;
		} catch(Exception e) {
		    return null;
	    }
	}

	public void writeData(Socket server, float d) {
	    try {
	        OutputStream socketStream = socket.getOutputStream();
	        ObjectOutputStream objectOutput = new ObjectOutputStream(socketStream);
	        objectOutput.writeObject(d);
	        objectOutput.close();
	        socketStream.close();
	    } catch (Exception e) {
	        System.out.println(e.toString());
	    }
	}
	public void run(){
    	while(true){
         	try{
	            System.out.println("Waiting for client on port " +serverSocket.getLocalPort() + "...");
	            Socket server = serverSocket.accept();
	            System.out.println("Just connected to "+ server.getRemoteSocketAddress());
	           	ArrayList<String> a = readData(server);
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
    	}
   	}
   	public static void main(String [] args){
    	int port = Integer.parseInt(args[0]);
      	try{
         	Thread t = new berkeleylm(port);
         	t.start();
      	}
      	catch(IOException e){
         	e.printStackTrace();
      	}
   	}
}