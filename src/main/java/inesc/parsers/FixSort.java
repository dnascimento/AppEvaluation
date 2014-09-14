package inesc.parsers;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FixSort {

    static Pattern pattern = Pattern.compile("Date=\\\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}\\\"");

    public static void main(String[] main) throws IOException, ParseException {
        File f = new File("/Volumes/backup/processed/perDay.txt");
        File fOut = new File("/Volumes/backup/processed/perDayFixed.txt");
        File fErr = new File("/Volumes/backup/processed/perDayError.txt");

        BufferedReader in = new BufferedReader(new FileReader(f));
        BufferedWriter out = new BufferedWriter(new FileWriter(fOut));
        BufferedWriter err = new BufferedWriter(new FileWriter(fErr));

        long processedLines = 0;
        String line;
        while ((line = in.readLine()) != null) {
            if (line.length() > 23) {
                Calendar date = getDate(line);
                if (date == null) {
                    // fix the date
                    String dateString = extractDate(line);
                    if (dateString == null) {
                        System.err.println("MERDA: " + line);
                        err.write(line);
                        err.newLine();
                        continue;
                    }
                    line = dateString + " " + line;
                }
                out.write(line);
                out.newLine();
                processedLines++;
            }
        }
        System.out.println(processedLines);
        in.close();
        out.close();
        err.close();
    }

    static String extractDate(String line) {
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            return matcher.group().substring(6, 29);
        }
        return null;
    }

    static Calendar getDate(String line) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss.SSS");
        String fullDate = line.substring(0, 23);
        Date date;
        try {
            date = dateFormat.parse(fullDate.replace("T", ""));
        } catch (ParseException e) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }
}
