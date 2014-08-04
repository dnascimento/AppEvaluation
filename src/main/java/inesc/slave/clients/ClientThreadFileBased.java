package inesc.slave.clients;

import inesc.slave.parsers.ParsePerDay;
import inesc.slave.parsers.ParsePerTopic;
import inesc.slave.parsers.StackOverflowParser;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.apache.http.impl.client.CloseableHttpClient;

public class ClientThreadFileBased extends
        ClientThread {

    StackOverflowParser parser;

    public ClientThreadFileBased(CloseableHttpClient httpClient, int clientId, ClientManager clientManager, File f, String targetHost) {
        super(httpClient, clientId, clientManager);
        if (f.getName().contains("Day")) {
            parser = new ParsePerDay(f, targetHost, this);
        } else {
            parser = new ParsePerTopic(f, targetHost, this);
        }
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
            log.info("Total requests: " + totalRequests);
        } catch (Exception e) {
            log.error(e);
        }
        collectStatistics();
        try {
            httpClient.close();
        } catch (IOException e) {
            log.error(e);
        }
    }





}
