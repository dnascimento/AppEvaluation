package pt.inesc.slave.clients;

import java.io.File;
import java.util.List;

import pt.inesc.parsers.ParsePerDay;
import pt.inesc.parsers.ParsePerTopic;
import pt.inesc.parsers.StackOverflowParser;

public class ClientThreadFileBased extends
        ClientThread {

    StackOverflowParser parser;

    public ClientThreadFileBased(int clientId,
            ClientManager clientManager,
            List<File> filesToExec,
            ClientConfiguration config,
            Integer numberOfLines,
            double readPercentage,
            boolean perTopic) {
        super(clientId, clientManager, config);
        if (perTopic) {
            parser = new ParsePerTopic(filesToExec, config.target, this, null, numberOfLines, readPercentage);
        } else {
            parser = new ParsePerDay(filesToExec, config.target, this, null, numberOfLines, readPercentage);
        }
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


    @Override
    public void over() {
        parser.over();
    }


}
