package pt.inesc.parsers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class SplitBy {

    static final String PREFIX = "week_";
    static String baseDir = "/Volumes/backup/all/perWeek/";
    static final int MAX_NUMBER = Integer.MAX_VALUE; // 52 weeks is one year
    private static final int MAX_REQUESTS = 1000000;

    final public static void main(String[] main) throws Exception {
        new SplitBy().run("/Volumes/backup/all/perDay.txt");
    }

    public void run(String inputFile) throws Exception {
        new File(baseDir).mkdir();

        StackStatistics stats = new StackStatistics();

        File f = new File(inputFile);
        BufferedReader in = new BufferedReader(new FileReader(f));
        int requests = 0;
        int currentWeek = 1;
        int weekCounter = 0;

        BufferedWriter out = getOutFile(Integer.toString(currentWeek));
        String line;

        while ((line = in.readLine()) != null) {
            if (line.length() == 0) {
                continue;
            }
            Calendar date;
            try {
                date = getDate(line);
            } catch (Exception e) {
                System.err.println(e);
                System.err.println(line);
                continue;
            }
            // if (date.get(Calendar.YEAR) != TARGET_YEAR) {
            // continue;
            // }

            // get week
            int week = date.get(Calendar.WEEK_OF_YEAR);

            // select file
            if (currentWeek != week) {
                weekCounter++;
                if (weekCounter == MAX_NUMBER) {
                    break;
                }
                out.close();
                currentWeek = week;
                out = getOutFile(Integer.toString(weekCounter));
            }
            if (requests++ == MAX_REQUESTS) {
                break;
            }

            // Get statistics
            String subLine = line.substring(24);
            char type = subLine.charAt(1);
            switch (type) {
            case 'a':
                stats.newAnswer(StackOverflowParser.getProperty(subLine, "Text"),
                                StackOverflowParser.getProperty(subLine, "Date"),
                                StackOverflowParser.getProperty(subLine, "Author"));
                break;
            case 'v':
                stats.newVotes(StackOverflowParser.getProperty(subLine, "AnswerId"));
                break;
            case 'c':
                stats.newComment(StackOverflowParser.getProperty(subLine, "Text"),
                                 StackOverflowParser.getProperty(subLine, "Date"),
                                 StackOverflowParser.getProperty(subLine, "Author"));
                break;
            case 'q':
                stats.newQuestion(StackOverflowParser.getProperty(subLine, "Date"),
                                  StackOverflowParser.getProperty(subLine, "Tags").split(","),
                                  StackOverflowParser.getProperty(subLine, "Text"),
                                  new Integer(StackOverflowParser.getProperty(subLine, "Views")),
                                  "",
                                  StackOverflowParser.getProperty(subLine, "Author"));
                break;
            default:
                System.out.println("Unknown type");
            }


            out.write(line);
            out.newLine();
        }
        out.close();
        in.close();
        BufferedWriter statsFile = getOutFile("stats");
        String statistics = stats.collect();
        statsFile.write(statistics);
        statsFile.close();
        System.out.println(statistics);
    }

    static BufferedWriter getOutFile(String week) throws IOException {
        return new BufferedWriter(new FileWriter(baseDir + PREFIX + week + ".txt"));
    }

    static Calendar getDate(String line) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss.SSS");
        String fullDate = line.substring(0, 23);
        Date date = dateFormat.parse(fullDate.replace("T", ""));
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }


}
