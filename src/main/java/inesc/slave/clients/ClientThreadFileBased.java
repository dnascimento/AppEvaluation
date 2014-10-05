package inesc.slave.clients;

import inesc.parsers.ParsePerDay;
import inesc.parsers.ParsePerTopic;
import inesc.parsers.StackOverflowParser;

import java.io.File;

public class ClientThreadFileBased extends
        ClientThread {

    StackOverflowParser parser;

    public ClientThreadFileBased(int clientId, ClientManager clientManager, File f, ClientConfiguration config) {
        super(clientId, clientManager, config);

        if (isTimeOrdered(f.getName())) {
            parser = new ParsePerDay(f, config.target, this, null);
        } else {
            parser = new ParsePerTopic(f, config.target, this, null);
        }
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
        log.info("Client" + clientId + "starting...");
        Thread.currentThread().setName("FileBasedThead " + clientId);
        try {
            parser.parseFile();
        } catch (Exception e) {
            log.error(e);
            e.printStackTrace();
        }
        try {
            end();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }





}
