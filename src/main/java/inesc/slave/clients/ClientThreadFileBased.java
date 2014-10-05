package inesc.slave.clients;

import inesc.parsers.ParsePerDay;
import inesc.parsers.ParsePerTopic;
import inesc.parsers.StackOverflowParser;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ClientThreadFileBased extends
        ClientThread {

    StackOverflowParser parser;

    public ClientThreadFileBased(int clientId, ClientManager clientManager, File f, URL targetHost, int throughput) {
        super(clientId, targetHost, clientManager, throughput);
        if (isTimeOrdered(f.getName())) {
            parser = new ParsePerDay(f, targetHost, this);
        } else {
            parser = new ParsePerTopic(f, targetHost, this);
        }
        executionTimes = new ArrayList<Short>();
        responseData = new ArrayList<ByteBuffer>();
    }

    private boolean isTimeOrdered(String fileName) {
        String name = fileName.toLowerCase();
        return !(name.contains("topic"));
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
            e.printStackTrace();
        }
        collectStatistics();
    }





}
