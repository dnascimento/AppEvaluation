package inesc.parsers;

import inesc.slave.RequestCreation;
import inesc.slave.clients.ClientThread;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Date;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.google.common.io.BaseEncoding;

public abstract class StackOverflowParser {
    File file;
    RequestCreation creator;
    private static Logger log = Logger.getLogger(StackOverflowParser.class);

    StackStatistics stats = new StackStatistics();

    ClientThread client;
    final String hostURL;
    Date parseStart = new Date();


    public StackOverflowParser(File f, URL targetHost, ClientThread client) {
        file = f;
        this.hostURL = targetHost.toString();
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
        boolean delete = (getProperty(line, "Delete") != null);
        boolean update = (getProperty(line, "Update") != null);

        stats.newComment(text, date, author);

        text = StringEscapeUtils.escapeHtml4(text);
        text = StringEscapeUtils.escapeJson(text);
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest((answerId + author + text).getBytes());
        String commentId = BaseEncoding.base64().encode(digest);

        if (delete) {
            return creator.deleteComment(hostURL, questionId, answerId, commentId);
        }
        if (update) {
            return creator.updateComment(hostURL, questionId, answerId, commentId, "This comment was attacked muahahahh!!!");
        }
        return creator.postComment(hostURL, questionId, answerId, text, author);
    }

    public HttpRequestBase newVote(String line) throws Exception {
        String questionId = getProperty(line, "QuestionId");
        String answerId = getProperty(line, "AnswerId");
        String date = getProperty(line, "Date");
        log.debug("New Vote: " + questionId + " " + answerId + " " + date);
        boolean down = (getProperty(line, "Down") != null);

        stats.newVotes(answerId);
        if (down) {
            return creator.voteDown(hostURL, questionId, answerId);
        } else {
            return creator.voteUp(hostURL, questionId, answerId);
        }
    }

    public HttpRequestBase newAnswer(String line) throws Exception {
        String questionId = getProperty(line, "QuestionId");
        String answerId = getProperty(line, "AnswerId");
        String author = getProperty(line, "Author");
        String date = getProperty(line, "Date");
        String text = getProperty(line, "Text");
        boolean delete = (getProperty(line, "Delete") != null);
        boolean update = (getProperty(line, "Update") != null);

        log.debug("New Answer: " + questionId + " " + answerId + " " + author + " " + date);

        stats.newAnswer(text, date, author);
        text = StringEscapeUtils.escapeHtml4(text);
        text = StringEscapeUtils.escapeJson(text);

        if (delete) {
            return creator.deleteAnswer(hostURL, questionId, answerId);
        }
        if (update) {
            return creator.updateAnswer(hostURL, questionId, answerId, "This answer was attacked muahahahh!!!");
        }

        return creator.postAnswer(hostURL, questionId, text, author, answerId);
    }

    public HttpRequestBase newQuestion(String line, String category) throws Exception {
        String title = getProperty(line, "Title");
        String tags = getProperty(line, "Tags");
        boolean delete = (getProperty(line, "Delete") != null);
        boolean update = (getProperty(line, "Update") != null);


        String questionId = getProperty(line, "QuestionId");
        String answerId = getProperty(line, "AnswerId");
        String author = getProperty(line, "Author");
        String date = getProperty(line, "Date");
        String text = getProperty(line, "Text");
        String views = getProperty(line, "Views");

        text = StringEscapeUtils.escapeHtml4(text);
        text = StringEscapeUtils.escapeJson(text);

        stats.newQuestion(date, tags.split(","), text, new Integer(views), category, author);
        log.debug("Question: " + questionId + " " + answerId + " " + author + " " + date + " " + views);

        if (delete) {
            return creator.deleteQuestion(hostURL, title);
        }
        if (update) {
            return creator.updateAnswer(hostURL, title, answerId, "This question was attacked muahahahh!!!");
        }

        return creator.postNewQuestion(hostURL, title, tags, text, author, views, "", answerId);

    }

    public boolean execRequest(HttpRequestBase req) {
        if (client != null) {
            return client.execRequest(req);
        }
        return false;
    }

    public static String getProperty(String line, String property) throws Exception {
        int start = line.indexOf(property + "=\"");
        if (start == -1) {
            if (property != "Delete" && property != "Down" && property != "Update") {
                log.error("Property not found: " + property + " in line " + line);
            }
            return null;
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
        System.out.println("start at " + new Date());
        File dir = new File("/Users/darionascimento/git/AppEvaluation/slave/");

        for (File f : dir.listFiles()) {
            if (f.getName().startsWith(".")) {
                continue;
            }
            StackOverflowParser parser;
            if (f.getName().contains("perTopic")) {
                parser = new ParsePerTopic(f, new URL("http://localhost:8080"), null);
            } else {
                parser = new ParsePerDay(f, new URL("http://localhost:8080"), null);
            }
            parser.parseFile();
        }
        System.out.println("Done at " + new Date());
    }

}
