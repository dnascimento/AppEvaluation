package pt.inesc.parsers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;


public class StackStatistics {
    File dir;

    private final Set<String> categories = new HashSet<String>();
    private final Set<String> tags = new HashSet<String>();
    private long totalQuestions = 0;
    private long totalAnswers = 0;
    private long totalComments = 0;

    private final Date snapshotDate = new Date(1399170868743L);

    private long totalVotes = 0;
    private long totalCharAnswers = 0;
    private long totalCharComments = 0;

    private final ArrayList<Integer> viewsPerQuestion = new ArrayList<Integer>();
    private final ArrayList<Integer> tagsPerQuestion = new ArrayList<Integer>();
    private final CounterPerAnswer answersPerQuestion = new CounterPerAnswer();
    private final CounterPerAnswer commentsPerAnswer = new CounterPerAnswer();
    private final CounterPerAnswer votesPerAnswer = new CounterPerAnswer();
    private final ArrayList<Integer> charsPerAnswer = new ArrayList<Integer>();
    private final ArrayList<Integer> charsPerComment = new ArrayList<Integer>();
    private final ArrayList<Integer> charsPerQuestion = new ArrayList<Integer>();
    private final ArrayList<Integer> viewsPerDay = new ArrayList<Integer>();

    private final HashMap<String, ActionsCounter> authors = new HashMap<String, ActionsCounter>();

    private final HashMap<String, Integer> commentsPerDay = new HashMap<String, Integer>();
    private final HashMap<String, Integer> answersPerDay = new HashMap<String, Integer>();
    private final HashMap<String, Integer> questionsPerDay = new HashMap<String, Integer>();

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss.SSS");

    private Date oldest;

    private Date newest;

    private long totalRequests;

    public void newVotes(String answerId) {
        votesPerAnswer.increment();
    }

    public void newComment(String text, String creationDate, String author) throws Exception {
        int textSize = text.length();
        String day = creationDate.split("T")[0];
        Integer ratePerDay = commentsPerDay.get(day);
        if (ratePerDay == null) {
            ratePerDay = 1;
        } else {
            ratePerDay += 1;
        }
        commentsPerDay.put(day, ratePerDay);
        Date date = dateFormat.parse(creationDate.replace("T", ""));

        ActionsCounter actionsCount = authors.get(author);
        if (actionsCount == null) {
            actionsCount = new ActionsCounter();
            authors.put(author, actionsCount);
        }
        actionsCount.comments++;
        actionsCount.total++;

        totalComments++;
        charsPerComment.add(textSize);
        commentsPerAnswer.increment();
    }



    public void newAnswer(String text, String creationDate, String author) throws Exception {
        int textSize = text.length();
        String day = creationDate.split("T")[0];
        Integer ratePerDay = answersPerDay.get(day);
        if (ratePerDay == null) {
            ratePerDay = 1;
        } else {
            ratePerDay += 1;
        }
        answersPerDay.put(day, ratePerDay);

        Date date = dateFormat.parse(creationDate.replace("T", ""));

        totalAnswers++;
        charsPerAnswer.add(textSize);
        commentsPerAnswer.reset();
        votesPerAnswer.reset();
        answersPerQuestion.increment();

        ActionsCounter actionsCount = authors.get(author);
        if (actionsCount == null) {
            actionsCount = new ActionsCounter();
            authors.put(author, actionsCount);
        }
        actionsCount.answers++;
        actionsCount.total++;
    }

    public void newQuestion(String fullDate, String[] tags, String text, int views, String category, String author) throws Exception {
        categories.add(category);
        int textSize = text.length();

        ActionsCounter actionsCount = authors.get(author);
        if (actionsCount == null) {
            actionsCount = new ActionsCounter();
            authors.put(author, actionsCount);
        }
        actionsCount.questions++;
        actionsCount.total++;

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
        answersPerQuestion.reset();
        for (String tag : tags) {
            this.tags.add(tag);
        }
    }





    private void printHashMap(PrintWriter out, String title, HashMap<String, Integer> map) {
        out.println("\n " + title);
        for (Entry<String, Integer> e : map.entrySet()) {
            out.println(e.getKey() + " , " + e.getValue());
        }
    }

    public long total() {
        for (Integer vote : votesPerAnswer.get()) {
            totalVotes += vote;
        }

        for (Integer chars : charsPerAnswer) {
            totalCharAnswers += chars;
        }

        for (Integer chars : charsPerComment) {
            totalCharComments += chars;
        }
        totalRequests = totalAnswers + totalComments + totalVotes + totalQuestions;
        return totalRequests;
    }

    public String collect() throws FileNotFoundException {
        total();
        StringBuilder sb = new StringBuilder();

        sb.append("\n Total authors: " + authors.size() + "\n");
        sb.append("Total categories: " + categories.size() + "\n");
        sb.append("Total tags: " + tags.size() + "\n");
        sb.append("Total Questios: " + totalQuestions + "\n");
        sb.append("Total Answers: " + totalAnswers + "\n");
        sb.append("Total Comments: " + totalComments + "\n");
        sb.append("Total Votes: " + totalVotes + "\n");
        sb.append("Total Char Answers: " + totalCharAnswers + "\n");
        sb.append("Total Char Comments: " + totalCharComments + "\n");
        if (oldest != null && newest != null) {
            sb.append("Oldest: " + oldest + "(" + oldest.getTime() + ")" + "\n");
            sb.append("Newest: " + newest + "(" + newest.getTime() + ")" + "\n");
            sb.append("Diff millisecounds: " + (newest.getTime() - oldest.getTime()) + "\n");
        }

        sb.append(avg("viewsPerDay", viewsPerDay) + "\n");
        sb.append(avg("viewsPerQuestion", viewsPerQuestion) + "\n");
        sb.append(avg("tagsPerQuestion", tagsPerQuestion) + "\n");
        sb.append(avg("answersPerQuestion", answersPerQuestion.get()) + "\n");
        sb.append(avg("commentsPerAnswer", commentsPerAnswer.get()) + "\n");
        sb.append(avg("votesPerAnswer", votesPerAnswer.get()) + "\n");
        sb.append(avg("charsPerAnswer", charsPerAnswer) + "\n");
        sb.append(avg("charsPerComment", charsPerComment) + "\n");
        sb.append(avg("charsPerQuestion", charsPerQuestion) + "\n");

        int totalActions = 0;
        for (Entry<String, ActionsCounter> counter : authors.entrySet()) {
            totalActions += counter.getValue().total;
        }
        sb.append("Authors, total actions: " + totalActions + " ," + authors.keySet().size() + " authors\n");
        int nAuthors = authors.keySet().size();
        int average = (nAuthors == 0) ? 0 : totalActions / nAuthors;
        String closest = "";
        HashSet<String> closers = new HashSet<String>();
        int difference = Integer.MAX_VALUE;

        int biggest = 0;
        String biggestName = "";

        for (Entry<String, ActionsCounter> e : authors.entrySet()) {
            int delta = Math.abs(e.getValue().total - average);
            if (e.getValue().questions < 3)
                continue;
            if (delta < difference) {
                closest = e.getKey();
                difference = delta;
                closers.clear();
            }
            if (delta == difference) {
                closers.add(closest);
            }
            if (e.getValue().total > biggest) {
                biggest = e.getValue().total;
                biggestName = e.getKey();
            }
        }

        closers.add(closest);
        for (String user : closers) {
            ActionsCounter c = authors.get(user);
            if (c == null)
                continue;
            sb.append("Average is " + average + ", closest author: " + user + ": with " + c.total + " total, " + c.answers + " answers, "
                    + c.comments + " comments, " + c.questions + " questions," + c.votes + " votes\n");
        }

        File stats = new File("rates.txt");
        PrintWriter out = new PrintWriter(stats);

        printHashMap(out, "questions", questionsPerDay);
        printHashMap(out, "answers", answersPerDay);
        printHashMap(out, "comments", commentsPerDay);
        out.close();



        return sb.toString();
    }

    private String avg(String title, ArrayList<Integer> list) {
        long total = 0;
        for (Integer i : list) {
            total += i;
        }
        double avg;

        if (list.size() == 0) {
            avg = total;
        } else {
            avg = total / list.size();
        }
        long totalSquare = 0;

        for (Integer i : list) {
            totalSquare += ((i - avg) * (i - avg));
        }
        double desvio;
        if (list.size() == 1) {
            desvio = 0;
        } else {
            desvio = Math.sqrt(totalSquare / (list.size() - 1));
        }
        return "Total " + title + " : " + total + " avg: " + avg + " desvio: " + desvio;

    }




    private class CounterPerAnswer {
        private final ArrayList<Integer> values = new ArrayList<Integer>(Arrays.asList(new Integer(0)));

        public void increment() {
            int index = values.size() - 1;
            int countVotes = values.get(index);
            values.set(index, countVotes + 1);
        }

        public void reset() {
            values.add(new Integer(0));
        }

        public ArrayList<Integer> get() {
            return values;
        }

    }

    class ActionsCounter {
        int total;
        int questions = 0;
        int answers = 0;
        int comments = 0;
        int votes = 0;
    }

}
