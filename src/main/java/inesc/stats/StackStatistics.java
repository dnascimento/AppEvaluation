package inesc.stats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;


public class StackStatistics {
    File dir;

    String currentQuestionTitle;
    String currentAnswerId;
    String AUTHOR = "crawler";

    Set<String> categories = new HashSet<String>();
    Set<String> tags = new HashSet<String>();
    long totalQuestions = 0;
    long totalAnswers = 0;
    long totalComments = 0;

    Date snapshotDate = new Date(1399170868743L);

    long totalVotes = 0;
    long totalCharAnswers = 0;
    long totalCharComments = 0;

    ArrayList<Integer> viewsPerQuestion = new ArrayList<Integer>();
    ArrayList<Integer> tagsPerQuestion = new ArrayList<Integer>();
    ArrayList<Integer> answersPerQuestion = new ArrayList<Integer>();
    ArrayList<Integer> commentsPerAnswer = new ArrayList<Integer>();
    ArrayList<Integer> votesPerAnswer = new ArrayList<Integer>();
    ArrayList<Integer> charsPerAnswer = new ArrayList<Integer>();
    ArrayList<Integer> charsPerComment = new ArrayList<Integer>();
    ArrayList<Integer> charsPerQuestion = new ArrayList<Integer>();
    ArrayList<Integer> viewsPerDay = new ArrayList<Integer>();

    HashMap<String, Integer> commentsPerDay = new HashMap<String, Integer>();
    HashMap<String, Integer> answersPerDay = new HashMap<String, Integer>();
    HashMap<String, Integer> questionsPerDay = new HashMap<String, Integer>();


    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss.SSS");

    private Date oldest;

    private Date newest;



    public static void main(String[] args) throws Exception {
        File dir = new File("/Volumes/DataDisk/data2/correct/");
        StackStatistics stats = new StackStatistics(dir);
        stats.parse();
        stats.collect();
        stats.printRatesPerDay();
    }


    private void printRatesPerDay() throws IOException {
        File stats = new File("rates.txt");
        PrintWriter out = new PrintWriter(stats);

        printHashMap(out, questionsPerDay);
        printHashMap(out, answersPerDay);
        printHashMap(out, commentsPerDay);
        out.close();
    }




    private void printHashMap(PrintWriter out, HashMap<String, Integer> map) {
        for (Entry<String, Integer> e : map.entrySet()) {
            out.println(e.getKey() + " , " + e.getValue());
        }
    }

    private void collect() {
        for (Integer vote : votesPerAnswer) {
            totalVotes += vote;
        }

        for (Integer chars : charsPerAnswer) {
            totalCharAnswers += chars;
        }

        for (Integer chars : charsPerComment) {
            totalCharComments += chars;
        }

        System.out.println("Total categories: " + categories.size());
        System.out.println("Total tags: " + tags.size());
        System.out.println("Total Questios: " + totalQuestions);
        System.out.println("Total Answers: " + totalAnswers);
        System.out.println("Total Comments: " + totalComments);
        System.out.println("Total Votes: " + totalVotes);
        System.out.println("Total Char Answers: " + totalCharAnswers);
        System.out.println("Total Char Comments: " + totalCharComments);
        System.out.println("Oldest: " + oldest + "(" + oldest.getTime() + ")");
        System.out.println("Newest: " + newest + "(" + newest.getTime() + ")");
        System.out.println("Diff millisecounds: " + (newest.getTime() - oldest.getTime()));

        avg("viewsPerDay", viewsPerDay);
        avg("viewsPerQuestion", viewsPerQuestion);
        avg("tagsPerQuestion", tagsPerQuestion);
        avg("answersPerQuestion", answersPerQuestion);
        avg("commentsPerAnswer", commentsPerAnswer);
        avg("votesPerAnswer", votesPerAnswer);
        avg("charsPerAnswer", charsPerAnswer);
        avg("charsPerComment", charsPerComment);
        avg("charsPerQuestion", charsPerQuestion);
    }

    private void avg(String title, ArrayList<Integer> list) {
        long total = 0;
        for (Integer i : list) {
            total += i;
        }
        double avg = total / list.size();
        long totalSquare = 0;

        for (Integer i : list) {
            totalSquare += ((i - avg) * (i - avg));
        }
        double desvio = Math.sqrt(totalSquare / (list.size() - 1));

        System.out.println("Total " + title + " : " + total + " avg: " + avg + " desvio: " + desvio);

    }

    private void parse() throws Exception {
        for (File f : dir.listFiles()) {
            parseFile(f);
        }

    }

    public StackStatistics(File dir) {
        this.dir = dir;
    }



    private void parseFile(File f) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line;
        while ((line = br.readLine()) != null) {
            try {
                String[] posts = line.split("<votes>");
                String category = posts[0].split("_")[0];
                categories.add(category);
                // parse posts
                for (int entryCounter = 1; entryCounter < posts.length; entryCounter++) {
                    String postWithComments = posts[entryCounter];
                    String[] entries = postWithComments.split("<row");
                    int votes = Integer.parseInt(entries[0].replace("</votes>  ", ""));
                    if (entryCounter == 1) {
                        newQuestion(entries[1]);
                    } else {
                        newAnswer(entries[1]);
                    }
                    newVotes(votes);
                    for (int j = 2; j < entries.length; j++) {
                        String comment = entries[j];
                        newComment(comment);
                    }
                }
            } catch (Exception e) {
                System.err.println(line);
                e.printStackTrace();
                System.err.println(e);
            }
        }
        br.close();
        System.out.println("File parsing over: " + f.getAbsolutePath());
    }

    private void newVotes(int votes) {
        votesPerAnswer.add(votes);
    }

    private void newComment(String comment) throws Exception {
        comment = comment.substring(0, comment.length() - 4);
        int textSize = getProperty(comment, "Text").length();
        String fullDate = getProperty(comment, "CreationDate");
        String day = fullDate.split("T")[0];
        Integer ratePerDay = commentsPerDay.get(day);
        if (ratePerDay == null) {
            ratePerDay = 1;
        } else {
            ratePerDay += 1;
        }
        commentsPerDay.put(day, ratePerDay);

        Date date = dateFormat.parse(fullDate.replace("T", ""));


        totalComments++;
        charsPerComment.add(textSize);
        int counter = commentsPerAnswer.get(commentsPerAnswer.size() - 1);
        commentsPerAnswer.set(commentsPerAnswer.size() - 1, ++counter);
    }



    private void newAnswer(String answer) throws Exception {
        int textSize = getProperty(answer, "Body").length();
        String fullDate = getProperty(answer, "CreationDate");
        String day = fullDate.split("T")[0];
        Integer ratePerDay = answersPerDay.get(day);
        if (ratePerDay == null) {
            ratePerDay = 1;
        } else {
            ratePerDay += 1;
        }
        answersPerDay.put(day, ratePerDay);

        Date date = dateFormat.parse(fullDate.replace("T", ""));


        totalAnswers++;
        charsPerAnswer.add(textSize);
        int counter = answersPerQuestion.get(answersPerQuestion.size() - 1);
        answersPerQuestion.set(answersPerQuestion.size() - 1, ++counter);
        commentsPerAnswer.add(0);
    }

    private void newQuestion(String question) throws Exception {
        String[] tags = escapeTags(getProperty(question, "Tags")).split(",");
        int textSize = getProperty(question, "Body").length();
        int views = Integer.parseInt(getProperty(question, "ViewCount"));

        String fullDate = getProperty(question, "CreationDate");
        String day = fullDate.split("T")[0];
        Integer ratePerDay = questionsPerDay.get(day);
        if (ratePerDay == null) {
            ratePerDay = 1;
        } else {
            ratePerDay += 1;
        }
        questionsPerDay.put(day, ratePerDay);

        Date date = dateFormat.parse(fullDate.replace("T", ""));

        if (oldest == null)
            oldest = date;

        if (newest == null)
            newest = date;


        if (date.before(oldest)) {
            oldest = date;
        }

        if (date.after(newest)) {
            newest = date;
        }
        long time = snapshotDate.getTime() - date.getTime();
        long days = time / (1000 * 3600 * 24);
        if (days == 0)
            days = 1;
        int viewsPerD = (int) (views / days);

        totalQuestions++;
        tagsPerQuestion.add(tags.length);
        viewsPerQuestion.add(views);
        charsPerQuestion.add(textSize);
        viewsPerDay.add(viewsPerD);
        answersPerQuestion.add(0);
        for (String tag : tags) {
            this.tags.add(tag);
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
            i = line.length() - 1;
        }
        return line.substring(start, i);
    }



    private static String escapeTags(String tags) {
        tags = StringEscapeUtils.unescapeHtml(tags);
        tags = tags.replaceAll("><", ",");
        tags = tags.replaceAll("<|>|\\.", "");
        return tags;
    }




}
