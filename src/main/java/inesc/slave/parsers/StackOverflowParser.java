package inesc.slave.parsers;

import inesc.slave.RequestCreation;
import inesc.slave.clients.ClientManager;
import inesc.slave.clients.ClientThread;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public abstract class StackOverflowParser {
    File file;
    RequestCreation creator;
    private static Logger log = Logger.getLogger(StackOverflowParser.class);

    StackStatistics stats = new StackStatistics();

    ClientThread client;
    final String hostURL;
    Date parseStart = new Date();


    public StackOverflowParser(File f, String hostURL, ClientThread client) {
        file = f;
        this.hostURL = hostURL;
        creator = new RequestCreation();
        this.client = client;
    }

    public abstract long parseFile() throws Exception;

    public HttpRequestBase newComment(String line) throws Exception {
        String questionId = getProperty(line, "QuestionId");
        String answerId = getProperty(line, "AnswerId");
        String author = getProperty(line, "Author");
        String date = getProperty(line, "Date");
        String text = getProperty(line, "Text");
        log.debug("New Comment: " + questionId + " " + answerId + " " + author + " " + date);

        stats.newComment(text, date);

        text = StringEscapeUtils.escapeHtml(text);
        return creator.postComment(hostURL, questionId, answerId, text, author);
    }


    public HttpRequestBase newVote(String line) throws Exception {
        String questionId = getProperty(line, "QuestionId");
        String answerId = getProperty(line, "AnswerId");
        String date = getProperty(line, "Date");
        log.debug("New Vote: " + questionId + " " + answerId + " " + date);

        stats.newVotes(answerId);

        return creator.voteUp(hostURL, questionId, answerId);
    }

    public HttpRequestBase newAnswer(String line) throws Exception {
        String questionId = getProperty(line, "QuestionId");
        String answerId = getProperty(line, "AnswerId");
        String author = getProperty(line, "Author");
        String date = getProperty(line, "Date");
        String text = getProperty(line, "Text");
        log.debug("New Answer: " + questionId + " " + answerId + " " + author + " " + date);

        stats.newAnswer(text, date);

        text = StringEscapeUtils.escapeHtml(text);

        return creator.postAnswer(hostURL, questionId, text, author, answerId);
    }

    public HttpRequestBase newQuestion(String line, String category) throws Exception {
        String title = getProperty(line, "Title");
        String tags = getProperty(line, "Tags");

        String questionId = getProperty(line, "QuestionId");
        String answerId = getProperty(line, "AnswerId");
        String author = getProperty(line, "Author");
        String date = getProperty(line, "Date");
        String text = getProperty(line, "Text");
        String views = getProperty(line, "Views");
        log.debug("New question: " + questionId + " " + answerId + " " + author + " " + date + " " + views);

        stats.newQuestion(date, tags.split(","), text, new Integer(views), category, author);

        return creator.postNewQuestion(hostURL, title, tags, text, author, views, "", answerId);
    }

    public void execRequest(HttpRequestBase req) {
        if (client != null) {
            client.execRequest(req);
        }

    }

    protected static String getProperty(String line, String property) throws Exception {
        int start = line.indexOf(property + "=\"");
        if (start == -1) {
            throw new Exception("Property not found: " + property + " in line " + line);
        }
        start += property.length() + 2;
        char[] str = line.toCharArray();
        int i = start;

        while (i < line.length() && str[i] != '\"') {
            i++;
        }
        if (line.length() == i) {
            throw new Exception("ERROR PARSING property: " + property + " in line: " + line);
        }
        return line.substring(start, i);
    }

    protected String summary() throws FileNotFoundException {
        StringBuilder sb = new StringBuilder();
        sb.append(stats.collect());
        sb.append("Duration (ms): " + (new Date().getTime() - parseStart.getTime()));
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        DOMConfigurator.configure("log4j.xml");
        log.setLevel(Level.ERROR);
        ClientManager manager = new ClientManager(null);
        System.out.println("start at " + new Date());
        File dir = new File("/Volumes/Untitled/go/");
        for (File f : dir.listFiles()) {
            if (f.getName().startsWith(".")) {
                continue;
            }
            // manager.newFile(f, "http://localhost:8080");
            if (f.getName().contains("perTopic")) {
                ParsePerTopic parser = new ParsePerTopic(f, "localhost:9000", null);
                parser.parseFile();
            } else {
                ParsePerDay parser = new ParsePerDay(f, "localhost:9000", null);
                parser.parseFile();
            }
        }
        System.out.println("Done at " + new Date());
        // manager.start();
    }

}
