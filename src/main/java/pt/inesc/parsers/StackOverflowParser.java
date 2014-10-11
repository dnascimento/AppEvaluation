package pt.inesc.parsers;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import pt.inesc.slave.RequestCreation;
import pt.inesc.slave.clients.ClientConfiguration;
import pt.inesc.slave.clients.ClientThread;
import pt.inesc.slave.clients.ReadTracker;
import pt.inesc.slave.clients.ReadTrackerIterator;

import com.google.common.io.BaseEncoding;

public abstract class StackOverflowParser {
    List<File> files;
    RequestCreation creator;
    private static Logger log = Logger.getLogger(StackOverflowParser.class);

    StackStatistics stats;

    ClientThread client;
    final String hostURL;
    Date parseStart = new Date();
    protected Integer numberOfLines;
    final int nReadsIn100Requests;
    int nWrites;
    ReadTracker questionsToRead;

    AtomicBoolean stop = new AtomicBoolean(false);


    public StackOverflowParser(List<File> filesToExec,
            HttpHost targetHost,
            ClientThread client,
            StackStatistics stats,
            Integer numberOfLines,
            double readPercentage) {
        files = filesToExec;
        this.hostURL = targetHost.toString();
        creator = new RequestCreation();
        this.client = client;
        this.stats = stats;
        this.numberOfLines = numberOfLines;
        this.nReadsIn100Requests = (int) (readPercentage * 100);
        nWrites = 100 - nReadsIn100Requests;
        if (nReadsIn100Requests != 0) {
            questionsToRead = new ReadTracker(nReadsIn100Requests);
        }
    }

    public abstract void parseFile() throws Exception;

    public HttpRequestBase newComment(String line) throws Exception {
        String questionId = cleanText(getProperty(line, "QuestionId"));
        String answerId = getProperty(line, "AnswerId");
        String author = getProperty(line, "Author");
        String date = getProperty(line, "Date");
        String text = cleanText(getProperty(line, "Text"));
        // log.debug("New Comment: " + questionId + " " + answerId + " " + author + " " +
        // date);
        boolean delete = (getProperty(line, "Delete") != null);
        boolean update = (getProperty(line, "Update") != null);

        if (stats != null)
            stats.newComment(text, date, author);

        text = StringEscapeUtils.escapeHtml4(text);
        text = StringEscapeUtils.escapeJson(text);
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest((answerId + author + text).getBytes());
        String commentId = BaseEncoding.base64().encode(digest);

        if (questionId == null) {
            return null;
        }


        if (delete) {
            return creator.deleteComment(hostURL, questionId, answerId, commentId);
        }
        if (update) {
            return creator.updateComment(hostURL, questionId, answerId, commentId, "This comment was attacked muahahahh!!!");
        }
        return creator.postComment(hostURL, questionId, answerId, text, author);
    }

    private String cleanText(String property) {
        if (property == null) {
            return null;
        }
        return property.replaceAll("[^A-Za-z0-9 ]", "a");
    }

    public HttpRequestBase newVote(String line) throws Exception {
        String questionId = cleanText(getProperty(line, "QuestionId"));
        String answerId = getProperty(line, "AnswerId");
        String date = getProperty(line, "Date");
        // log.debug("New Vote: " + questionId + " " + answerId + " " + date);
        boolean down = (getProperty(line, "Down") != null);

        if (stats != null)
            stats.newVotes(answerId);

        if (questionId == null) {
            return null;
        }


        if (down) {
            return creator.voteDown(hostURL, questionId, answerId);
        } else {
            return creator.voteUp(hostURL, questionId, answerId);
        }
    }

    public HttpRequestBase newAnswer(String line) throws Exception {
        String questionId = cleanText(getProperty(line, "QuestionId"));
        String answerId = getProperty(line, "AnswerId");
        String author = getProperty(line, "Author");
        String date = getProperty(line, "Date");
        String text = cleanText(getProperty(line, "Text"));
        boolean delete = (getProperty(line, "Delete") != null);
        boolean update = (getProperty(line, "Update") != null);

        // log.debug("New Answer: " + questionId + " " + answerId + " " + author + " " +
        // date);

        if (stats != null)
            stats.newAnswer(text, date, author);

        text = StringEscapeUtils.escapeHtml4(text);
        text = StringEscapeUtils.escapeJson(text);
        if (questionId == null) {
            return null;
        }


        if (delete) {
            return creator.deleteAnswer(hostURL, questionId, answerId);
        }
        if (update) {
            return creator.updateAnswer(hostURL, questionId, answerId, "This answer was attacked muahahahh!!!");
        }

        return creator.postAnswer(hostURL, questionId, text, author, answerId);
    }

    public HttpRequestBase newQuestion(String line, String category) throws Exception {
        String title = cleanText(getProperty(line, "Title"));
        String tags = getProperty(line, "Tags");
        boolean delete = (getProperty(line, "Delete") != null);
        boolean update = (getProperty(line, "Update") != null);


        // String questionId = cleanText(getProperty(line, "QuestionId"));
        String answerId = getProperty(line, "AnswerId");
        String author = getProperty(line, "Author");
        String date = getProperty(line, "Date");
        String text = cleanText(getProperty(line, "Text"));
        String views = getProperty(line, "Views");

        text = StringEscapeUtils.escapeHtml4(text);
        text = StringEscapeUtils.escapeJson(text);

        if (stats != null)
            stats.newQuestion(date, tags.split(","), text, new Integer(views), category, author);

        if (title == null) {
            return null;
        }


        if (delete) {
            return creator.deleteQuestion(hostURL, title);
        }
        if (update) {
            return creator.updateAnswer(hostURL, title, answerId, "This question was attacked muahahahh!!!");
        }

        if (questionsToRead != null) {
            questionsToRead.newQuestion(title);
        }
        return creator.postNewQuestion(hostURL, title, tags, text, author, views, "", answerId);

    }

    public void execRequest(HttpRequestBase req) {
        if (client != null) {
            client.execRequest(req);
        }
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
        if (stats != null)
            sb.append(stats.collect());

        sb.append("Duration (ms): " + (new Date().getTime() - parseStart.getTime()));
        return sb.toString();
    }

    /**
     * Main class to process the files and collect their statistics
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        DOMConfigurator.configure("log4j.xml");
        log.setLevel(Level.ERROR);
        System.out.println("start at " + new Date());
        File dir = new File("/Users/darionascimento/dataDisk/20million/merge");

        List<File> files = new ArrayList<File>();
        for (File f : dir.listFiles()) {
            if (f.getName().startsWith(".")) {
                continue;
            }
            files.add(f);
        }
        StackOverflowParser parser;
        StackStatistics stats = new StackStatistics();
        if (files.get(0).getName().contains("perTopic")) {
            parser = new ParsePerTopic(files, new HttpHost("localhost", 8080), null, stats, ClientConfiguration.ALL_LINES, 0);
        } else {
            parser = new ParsePerDay(files, new HttpHost("localhost", 8080), null, stats, ClientConfiguration.ALL_LINES, 0);
        }
        parser.parseFile();
        System.out.println(stats.collect());
        System.out.println("Done at " + new Date());
    }

    void performReads() {
        if (questionsToRead.isEmpty()) {
            return;
        }
        ReadTrackerIterator it = questionsToRead.getInfinitIterator();

        for (int i = 0; i < nReadsIn100Requests; i++) {
            String s = it.next();
            execRequest(creator.getQuestion(hostURL, s));
        }
    }

    public void over() {
        stop.set(true);
    }
}
