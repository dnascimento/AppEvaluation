package inesc;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class DumpServer {
    static ServerSocket server;

    public static void main(String[] args) throws Exception {
        server = new ServerSocket(8080);
        Socket s = server.accept();
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        String line;
        while ((line = in.readLine()) != null) {
            System.out.println(line);
        }
    }
}
