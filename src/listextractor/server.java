package listextractor;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import listextractor.extract;

public class server{
	
    public static void main(String[] args) throws Exception {
        HttpServer wserver = HttpServer.create(new InetSocketAddress(8000), 0);
        MyHandler mh = new MyHandler();
        wserver.createContext("/listextractor", mh);
        wserver.setExecutor(null);
        server s = new server();
        wserver.start();
    }

    static class MyHandler implements HttpHandler {
	    extract e;
		public MyHandler() throws Exception{
			e = new extract();
		}
		public String lists(HttpExchange t) throws Exception{
			java.util.Scanner scan = new java.util.Scanner(t.getRequestBody()).useDelimiter("\\A");
			String inp = "";
			while(scan.hasNext()) inp += scan.next() + "\n";
			e.process(inp);
			String[][][] s = e.lists(); String ret ="";
			for(int i=0;i<s.length;i++){
				ret += "Sentence #"+i+":\n";
				for(int j=0;j<s[i].length;j++){
					ret += "List #"+j+":\n";
					for(int k=0;k<s[i][j].length;k++){
						ret += s[i][j][k] + "\n";
					}
					ret += "\n";
				}
				ret += "\n";
			}
			return ret;
		}
        public void handle(HttpExchange t) throws IOException {
            String response = "";
            try{
            	response = lists(t);
            }
            catch(Exception e){
            	e.printStackTrace();
            }
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

}