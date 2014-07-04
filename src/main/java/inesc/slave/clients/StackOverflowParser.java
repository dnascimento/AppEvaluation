package inesc.slave.clients;

import inesc.slave.RequestCreation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

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
                        // answerCount = client.execRequest(newQuestion(question));
                        client.execRequest(newQuestion(question));
                        totalRequests++;
                    } else {
                        String answer = entries[1];
                        // TODO author?
                        client.execRequest(newAnswer(answer, "author"));
                        totalRequests++;
                        // answerCount--;
                    }
                    client.execRequest(newVote(votes));
                    totalRequests++;
                    for (int j = 2; j < entries.length; j++) {
                        String comment = entries[j];
                        client.execRequest(newComment(comment));
                        totalRequests++;
                    }
                }
                // if (answerCount != 0) {
                // System.out.println(line);
                // throw new Exception("WRONG NUMBER OF ANSWERS: " + answerCount);
                // }
            } catch (Exception e) {
                log.error(e);
            }
        }
        br.close();
        return totalRequests;
    }

    private HttpRequestBase newComment(String comment) throws Exception {
        comment = comment.substring(0, comment.length() - 4);
        String text = getProperty(comment, "Text");
        return creator.postComment(hostURL, currentQuestionTitle, currentAnswerId, text);
    }


    private HttpRequestBase newVote(int votes) {
        return creator.voteUp(hostURL, currentQuestionTitle, currentAnswerId);
    }

    private HttpRequestBase newAnswer(String answer, String author) throws Exception {
        // String tags = getProperty(answer,"CreationDate");
        // String score = getProperty(answer, "Score");
        String text = getProperty(answer, "Body");
        currentAnswerId = creator.generateAnswerId(currentQuestionTitle, author, answer);
        return creator.postAnswer(hostURL, currentQuestionTitle, text, author);
    }

    private HttpRequestBase newQuestion(String question) throws Exception {
        // String tags = getProperty(question,"CreationDate");
        // String score = getProperty(question, "Score");
        String tags = getProperty(question, "Tags");
        String text = getProperty(question, "Body");
        String title = getProperty(question, "Title");
        String answers = getProperty(question, "AnswerCount");
        String views = getProperty(question, "ViewCount");

        currentQuestionTitle = title;
        return creator.postNewQuestion(hostURL, title, tags, text);
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



}
