package inesc.slave.clients;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.apache.http.impl.client.CloseableHttpClient;

public class ClientThreadFileBased extends
        ClientThread {

    StackOverflowParser parser;

    public ClientThreadFileBased(CloseableHttpClient httpClient, int clientId, ClientManager clientManager, File f, String targetHost) {
        super(httpClient, clientId, clientManager);
        parser = new StackOverflowParser(f, targetHost, this);

        executionTimes = new ArrayList<Short>();
        responseData = new ArrayList<ByteBuffer>();
    }

    /**
     * Executes the GetMethod and prints status information.
     */
    @Override
    public void run() {
        initStatistics();
        log.info("Client" + clientID + "starting...");
        try {
            totalRequests = parser.parseFile();
        } catch (Exception e) {
            log.error(e);
        }
        collectStatistics();
    }


}
