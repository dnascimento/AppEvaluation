package inesc.slave.clients;

import inesc.slave.RequestCreation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.log4j.Logger;

public class StackOverflowParser {
    File file;
    RequestCreation creator;
    private static Logger log = Logger.getLogger(StackOverflowParser.class);

    String currentQuestionTitle;
    String currentAnswerId;
    String AUTHOR = "crawler";
    ClientThread client;
    final String hostURL;


    int totalRequests = 0;

    public StackOverflowParser(File f, String hostURL, ClientThread client) {
        file = f;
        this.hostURL = hostURL;
        creator = new RequestCreation();
        this.client = client;
    }

    public int parseFile() throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        log.info("Parsing file: " + file.getAbsolutePath() + "...");
        while ((line = br.readLine()) != null) {
            try {
                String[] posts = line.split("<votes>");
                String category = posts[0];
                System.out.println("cat " + category);
                // parse posts
                for (int entryCounter = 1; entryCounter < posts.length; entryCounter++) {
                    String postWithComments = posts[entryCounter];
                    String[] entries = postWithComments.split("<row");
                    int votes = Integer.parseInt(entries[0].replace("</votes>  ", ""));
                    if (entryCounter == 1) {
                        String question = entries[1];
                        client.execRequest(newQuestion(question));
                        totalRequests++;
                    } else {
                        String answer = entries[1];
                        client.execRequest(newAnswer(answer));
                        totalRequests++;
                    }
                    newVote(votes);
                    for (int j = 2; j < entries.length; j++) {
                        String comment = entries[j];
                        client.execRequest(newComment(comment));
                        totalRequests++;
                    }
                }
            } catch (Exception e) {
                log.error(line);
                log.error(e);
            }
        }
        br.close();
        log.info("File parsing over: " + file.getAbsolutePath());
        return totalRequests;
    }

    private HttpRequestBase newComment(String comment) throws Exception {
        comment = comment.substring(0, comment.length() - 4);
        String text = getProperty(comment, "Text");
        return creator.postComment(hostURL, currentQuestionTitle, currentAnswerId, text, AUTHOR);
    }


    private void newVote(int votes) {
        for (int i = 0; i < votes; i++) {
            client.execRequest(creator.voteUp(hostURL, currentQuestionTitle, currentAnswerId));
            totalRequests++;
        }
    }

    private HttpRequestBase newAnswer(String answer) throws Exception {
        // String tags = getProperty(answer,"CreationDate");
        // String score = getProperty(answer, "Score");
        String text = getProperty(answer, "Body");
        currentAnswerId = creator.generateAnswerId(currentQuestionTitle, AUTHOR, text);
        return creator.postAnswer(hostURL, currentQuestionTitle, text, AUTHOR);
    }

    private HttpRequestBase newQuestion(String question) throws Exception {
        // String tags = getProperty(question,"CreationDate");
        // String score = getProperty(question, "Score");
        String tags = escapeTags(getProperty(question, "Tags"));
        String text = getProperty(question, "Body");
        String title = escapeTitle(getProperty(question, "Title"));
        String answers = getProperty(question, "AnswerCount");
        String views = getProperty(question, "ViewCount");
        currentQuestionTitle = title;
        currentAnswerId = creator.generateAnswerId(currentQuestionTitle, AUTHOR, text);
        return creator.postNewQuestion(hostURL, title, tags, text, AUTHOR);
    }


    public static void main(String[] args) throws Exception {
        File dir = new File("/Volumes/DataDisk/data");
        ClientManager manager = new ClientManager(null);
        for (File f : dir.listFiles()) {
            manager.newFile(f, "http://localhost:8080");
            manager.start();
            manager.restart();
        }
    }

    private static String getProperty(String line, String property) throws Exception {
        int start = line.indexOf(property + "=\"");
        if (start == -1) {
            throw new Exception("Property not found");
        }
        start += property.length() + 2;
        char[] str = line.toCharArray();
        int i = start;

        while (i < line.length() && str[i] != '\"') {
            i++;
        }
        if (line.length() == i) {
            throw new Exception("ERROR PARSING property");
        }
        return line.substring(start, i);
    }

    // only ASCII
    private static String escapeTitle(String title) {
        return title.replaceAll("\\?|[^\\p{ASCII}]|\\.|\\\'", "");
    }

    private static String escapeTags(String tags) {
        tags = StringEscapeUtils.unescapeHtml4(tags);
        tags = tags.replaceAll("><", ",");
        tags = tags.replaceAll("<|>", "");
        return tags;
    }


}
