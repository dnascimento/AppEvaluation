package inesc.slave.parsers;

import inesc.slave.clients.ClientThread;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.log4j.Logger;

public class ParsePerDay extends
        StackOverflowParser {

    public ParsePerDay(File f, String hostURL, ClientThread client) {
        super(f, hostURL, client);
    }

    private static Logger log = Logger.getLogger(ParsePerDay.class);

    @Override
    public long parseFile() throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        log.info("Parsing file: " + file.getAbsolutePath() + "...");
        while ((line = br.readLine()) != null) {
            if (line.length() == 0) {
                continue;
            }
            try {
                char[] lineChars = line.toCharArray();
                if (lineChars[0] != '<') {
                    // new date
                    String date = line.substring(0, 24);
                    log.debug(date);
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
                    execRequest(newQuestion(line));
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
            }
        }
        br.close();
        log.fatal(summary());
        return votesStat + questionsStat + answersStat + commentsStat;
    }

}
