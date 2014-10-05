package inesc.parsers;

import inesc.slave.clients.ClientThread;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.http.HttpHost;
import org.apache.log4j.Logger;

public class ParsePerTopic extends
        StackOverflowParser {

    private static Logger log = Logger.getLogger(ParsePerTopic.class);


    public ParsePerTopic(File f, HttpHost targetHost, ClientThread client, StackStatistics stats) {
        super(f, targetHost, client, stats);
    }


    @Override
    public void parseFile() throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        log.info("Parsing file: " + file.getAbsolutePath() + "...");
        while ((line = br.readLine()) != null) {
            if (line.length() == 0) {
                continue;
            }
            try {
                int start = line.indexOf('<');
                if (start != 0) {
                    String category = line.substring(0, start);
                    category = category.split("_")[0];
                    // new question
                    execRequest(newQuestion(line.substring(start), category));
                } else {
                    char type = line.toCharArray()[1];
                    switch (type) {
                    case 'a':
                        execRequest(newAnswer(line));
                        break;
                    case 'v':
                        execRequest(newVote(line));
                        break;
                    case 'c':
                        execRequest(newComment(line));
                        break;
                    case '/':
                        // end mark
                        break;
                    default:
                        log.error("Unknown type: " + line);
                        continue;
                    }
                }
            } catch (Exception e) {
                log.error(line);
                log.error(e);
                e.printStackTrace();
            }
        }
        br.close();
    }
}
