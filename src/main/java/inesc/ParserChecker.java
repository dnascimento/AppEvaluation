package inesc;

import inesc.slave.RequestCreation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

import org.apache.http.client.methods.HttpRequestBase;

public class ParserChecker {
    String filename;
    RequestCreation creator = new RequestCreation();

    String currentQuestionTitle;
    String currentAnswerId;

    int totalRequests = 0;

    public ParserChecker(String file) {
        filename = file;
    }

    public int parseFile() throws Exception {
        PrintWriter errorFile = new PrintWriter(filename + "error.txt");
        PrintWriter correctFile = new PrintWriter(filename + ".txt");

        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
            try {
                String[] posts = line.split("<votes>");
                String category = posts[0];
                int answerCount = 0;
                // parse posts
                for (int entryCounter = 1; entryCounter < posts.length; entryCounter++) {
                    String postWithComments = posts[entryCounter];
                    String[] entries = postWithComments.split("<row");
                    int votes = Integer.parseInt(entries[0].replace("</votes>  ", ""));
                    if (entryCounter == 1) {
                        String question = entries[1];
                        answerCount = newQuestion(question);
                        totalRequests++;
                    } else {
                        String answer = entries[1];
                        newAnswer(answer, "author");
                        totalRequests++;
                        answerCount--;
                    }
                    newVote(votes);
                    totalRequests++;
                    for (int j = 2; j < entries.length; j++) {
                        String comment = entries[j];
                        newComment(comment);
                        totalRequests++;
                    }
                }
                if (answerCount != 0) {
                    System.out.println(line);
                    throw new Exception("WRONG NUMBER OF ANSWERS: " + answerCount);
                }
                correctFile.println(line);
            } catch (Exception e) {
                System.err.println(e);
                errorFile.println(line);
            }
        }
        br.close();
        errorFile.close();
        correctFile.close();
        return totalRequests;
    }

    private HttpRequestBase newComment(String comment) throws Exception {
        comment = comment.substring(0, comment.length() - 4);
        String text = getProperty(comment, "Text");
        return null;
    }


    private HttpRequestBase newVote(int votes) {
        return null;
    }

    private HttpRequestBase newAnswer(String answer, String author) throws Exception {
        // String tags = getProperty(answer,"CreationDate");
        // String score = getProperty(answer, "Score");
        String text = getProperty(answer, "Body");
        currentAnswerId = creator.generateAnswerId(currentQuestionTitle, author, answer);
        return null;
    }

    private int newQuestion(String question) throws Exception {
        // String tags = getProperty(question,"CreationDate");
        // String score = getProperty(question, "Score");
        String tags = getProperty(question, "Tags");
        String text = getProperty(question, "Body");
        String title = getProperty(question, "Title");
        int count = Integer.parseInt(getProperty(question, "AnswerCount"));
        String views = getProperty(question, "ViewCount");
        currentQuestionTitle = title;
        return count;
    }

    public static void main(String[] args) throws Exception {
        File dir = new File("/Volumes/DataDisk/data");
        for (File f : dir.listFiles()) {
            new ParserChecker(f.getAbsolutePath()).parseFile();
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
