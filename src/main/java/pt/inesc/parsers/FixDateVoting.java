package pt.inesc.parsers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;


public class FixDateVoting {

    static Pattern pattern = Pattern.compile("Date=\\\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}\\\"");

    public static void main(String[] main) throws Exception {
        new FixDateVoting().run("/Volumes/backup/processed/perDaySorted.txt");
        System.out.println("Sorting...");
        Process p = Runtime.getRuntime()
                           .exec("sort -T /Volumes/backup/processed/tmp /Volumes/backup/processed/perDayFixed.txt -o /Volumes/backup/processed/sorted.txt");
        int i = p.waitFor();
        System.out.println("Splitting...");

        new SplitBy().run("/Volumes/backup/processed/sorted.txt");
    }

    public void run(String inputFile) throws IOException {
        File f = new File(inputFile);
        File fOut = new File("/Volumes/backup/processed/perDayFixed.txt");
        File fErr = new File("/Volumes/backup/processed/perDayError.txt");

        BufferedReader in = new BufferedReader(new FileReader(f));
        BufferedWriter out = new BufferedWriter(new FileWriter(fOut));
        BufferedWriter err = new BufferedWriter(new FileWriter(fErr));

        long processedLines = 0;
        String line;
        char[] replace = "23:59:59.999".toCharArray();
        while ((line = in.readLine()) != null) {
            if (line.length() > 23) {
                String hour = line.substring(11, 23);
                if ("00:00:00.000".equals(hour)) {
                    // fix the hour to latest moment in day
                    char[] larray = line.toCharArray();
                    for (int i = 0; i < replace.length; i++) {
                        larray[11 + i] = replace[i];
                    }
                    line = new String(larray);
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
}
