package pt.inesc.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.log4j.Logger;

import pt.inesc.slave.clients.ClientThread;

public class ParsePerTopic extends
        StackOverflowParser {

    private static Logger log = Logger.getLogger(ParsePerTopic.class);


    public ParsePerTopic(List<File> filesToExec,
            HttpHost targetHost,
            ClientThread client,
            StackStatistics stats,
            Integer numberOfLines,
            double readPercentage) {
        super(filesToExec, targetHost, client, stats, numberOfLines, readPercentage);
    }

    @Override
    public void parseFile() throws Exception {
        for (File file : files) {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            log.info("Parsing file: " + file.getAbsolutePath() + "...");
            while ((line = br.readLine()) != null) {
                if (line.length() == 0) {
                    continue;
                }
                if (numberOfLines-- == 0) {
                    break;
                }

                if (nReadsIn100Requests != 0 && nWrites-- == 0) {
                    performReads();
                    nWrites = 100 - nReadsIn100Requests;
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


}
