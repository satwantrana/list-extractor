package listextractor;

import org.deeplearning4j.util.SerializationUtils;

import java.net.*;
import java.io.*;
import java.util.zip.*;
import java.util.*;
import java.lang.Math;

public class word2vec extends Thread{
    class wordvector{
	double arr[][];
	int N, M;
	HashMap <String, Integer> map;
	public wordvector(String file) throws IOException{
	    map = new HashMap<String, Integer>();
	    //BufferedReader reader = File.newBufferedReader(file);
	    //FileInputStream inputStream = new FileInputStream(path);
	    //Scanner input = new Scanner(inputStream, "UTF-8");
	    BufferedReader input = new BufferedReader(new FileReader(new File(file)));
	    //System.out.println("Got the file");
	    String vals[] = input.readLine().split(" ");
	    //System.out.println(vals[0]+" "+vals[1]);
	    N = Integer.parseInt(vals[0]);
	    M = Integer.parseInt(vals[1]);

	    arr = new double[N][M];
	    //System.out.println(N + " : " + M);
	    for(int i=0;i<N;i++){
		String parts[] = input.readLine().split(" ");
		String word = parts[0];
		//System.out.println(i + " : " + word + " " + parts.length);
		map.put(word, i);
		for(int j=0;j+1<parts.length;j++){
		    arr[i][j] = Double.parseDouble(parts[j+1]);
		}
		//System.out.println("looped");
	    }
	}

	public double cosine(String a, String b){
	    a = a.toLowerCase();
	    b = b.toLowerCase();
	    if( !map.containsKey(a) || !map.containsKey(b))
		return Math.log(1e-1); 
	    int i = map.get(a);
	    int j = map.get(b);
	    double ret = 0;
	    double A = 0, B = 0;
	    for(int k=0;k<M;k++){
		A += arr[i][k] * arr[i][k];
		B += arr[j][k] * arr[j][k];
		ret += arr[i][k] * arr[j][k];
	    }
	    A = Math.sqrt(A);
	    B = Math.sqrt(B);
	    //System.out.println(a+" "+b+" "+A+" "+B+" "+ret);
	    return Math.log(ret/ (A * B));
	}
    }
    wordvector vec;
    private ServerSocket serverSocket;
    
	public word2vec(String file,int port) throws Exception{
	    // vec = Word2VecLoader.loadGoogleModel(file);
	    //System.out.println("attempting to load the vector file: " + file);
	    vec = new wordvector(file); 
	    //System.out.println("loaded vector file"); 
	    serverSocket = new ServerSocket(port);
	    //System.out.println("created the socket"); 
	}
	public Double wordsim(String a, String b){
		//return 1.23;
		//System.out.println(Math.log(vec.similarity(a,b)));
	    return vec.cosine(a,b); 
	}
	public ArrayList<String> readData(Socket server) {
	    try {
		    InputStream socketStream = server.getInputStream();
		    ObjectInputStream objectInput = new ObjectInputStream(new GZIPInputStream(socketStream));
		    //System.out.println("Reading from server");
		    ArrayList<String> a = (ArrayList<String>) objectInput.readObject();
		    objectInput.close();
		    socketStream.close();
		    //System.out.println("Read from server: "+a);
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
	        //System.out.println("Writing from server");
	        objectOutput.writeObject(d);
	        objectOutput.close();
	        socketStream.close();
	        //System.out.println("Written from server: "+d);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	public void run(){
	    while(true){
         	try{
	            //System.out.println("Waiting for client on port " +serverSocket.getLocalPort() + "...");
	            Socket server = serverSocket.accept();
	            //System.out.println("Just connected to "+ server.getRemoteSocketAddress());
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
	    System.out.println(args[0]);
	    System.out.println(port); 
	    Thread t = new word2vec(args[0],port);
	    System.out.println("Word2Vec Server Ready.");
	    t.start();
      	}
      	catch(IOException e){
	    e.printStackTrace();
      	}
    }
}

