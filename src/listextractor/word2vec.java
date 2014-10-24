package listextractor;

import org.deeplearning4j.util.SerializationUtils;
import org.deeplearning4j.word2vec.Word2Vec;
import org.deeplearning4j.word2vec.loader.Word2VecLoader;

import java.net.*;
import java.io.*;

public class word2vec{
	Word2Vec vec;
	private ServerSocket serverSocket;

	public similarity(String file,String port) throws Exception{
		vec = Word2VecLoader.loadGoogleModel(file);
		serverSocket = new ServerSocket(port);
      	serverSocket.setSoTimeout(10000);
	}
	public Double wordsim(String a, String b){
		return Math.log(vec.similarity(a,b));
	}
	public String[] readData(Socket server) {
	    try {
		    InputStream socketStream = server.getInputStream();
		    ObjectInputStream objectInput = new ObjectInputStream(new GZIPInputStream(socketStream));
		    String[] a = (String[]) objectInput.readObject();
		    return a;
		} catch(Exception e) {
		    return null;
	    }
	}
	public void writeData(Socket server, Double d) {
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
	           	String[] a = readData(server);
	            writeData(server, wordsim(a[0],a[1]));
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
         	Thread t = new word2vec(port);
         	t.start();
      	}
      	catch(IOException e){
         	e.printStackTrace();
      	}
   	}
}