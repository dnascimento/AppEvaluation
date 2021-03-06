package pt.inesc.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.log4j.Logger;

import pt.inesc.slave.clients.ClientThread;

public class ParsePerDay extends
        StackOverflowParser {


    public ParsePerDay(List<File> filesToExec,
            HttpHost targetHost,
            ClientThread client,
            StackStatistics stats,
            Integer numberOfLines,
            double readPercentage) {
        super(filesToExec, targetHost, client, stats, numberOfLines, readPercentage);
    }

    private static Logger log = Logger.getLogger(ParsePerDay.class);

    @Override
    public void parseFile() throws Exception {
        for (File file : files) {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            log.info("Parsing file: " + file.getAbsolutePath() + "...");
            while ((line = br.readLine()) != null & !stop.get()) {
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
                    char[] lineChars = line.toCharArray();
                    if (lineChars[0] != '<') {
                        // new date
                        // String date = line.substring(0, 24);
                        line = line.substring(24);
                        lineChars = line.toCharArray();
                    }
                    char type = lineChars[1];
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
                    case 'q':
                        execRequest(newQuestion(line, ""));
                        break;
                    case '/':
                        // end;
                        break;
                    default:
                        log.error("Unknown type: " + type + " in line " + line);
                        continue;
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
