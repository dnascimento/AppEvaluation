package pt.inesc.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class SplitFile {

    static String filePrefix = "part_";

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("You must specify the file to split");
        }
        BufferedReader in = new BufferedReader(new FileReader(new File(args[0])));
        long SPLIT = Long.valueOf(args[1]);
        String line;
        long counter = 0;
        int fileId = 0;
        boolean shallSplit = false;
        BufferedWriter out = new BufferedWriter(new FileWriter(fileId + ".txt"));
        while ((line = in.readLine()) != null) {
            counter++;

            if (counter % SPLIT == 0) {
                shallSplit = true;
            }
            if (shallSplit) {
                if (line.length() > 0 && line.toCharArray()[0] != '<') {
                    out.close();
                    fileId++;
                    out = new BufferedWriter(new FileWriter(fileId + ".txt"));
                    shallSplit = false;
                }
            }
            out.write(line + "\n");
        }

        in.close();
    }
}
