/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */

package pt.inesc.master;


import inesc.shared.AppEvaluationProtos.HistoryMsg.AppRequest;

import java.io.File;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.apache.http.HttpHost;
import org.apache.log4j.Logger;

import pt.inesc.master.commands.AskIntBuffers;
import pt.inesc.slave.clients.ClientConfiguration;
import de.svenjacobs.loremipsum.LoremIpsum;

public class Interface extends
        Thread {
    private static final int RANDOM_TEXT_SIZE = 20;
    private static final String BASE_DIR = "/slave";
    private final Master master;
    String lastTitle;
    String lastAnswerId;

    AskIntBuffers bufferCreator = new AskIntBuffers();
    private static Logger log = Logger.getLogger(Interface.class);
    LoremIpsum loremIpsum = new LoremIpsum();
    String tags = "ist";
    String AUTHOR = "interface_author";
    private int lorumPosition = 0;
    private final String SERVER_URL = "http://localhost:9000";

    public Interface(Master master) {
        this.master = master;
    }



    @Override
    public void run() {
        Scanner s = new Scanner(System.in);
        String cmd;

        while (true) {
            try {
                System.out.println("a) Add new story");
                System.out.println("b) Send file");
                System.out.println("c) Start stories");
                cmd = s.next();

                System.out.println("Enter the target host: ");
                String dest = s.next();
                HttpHost target = new HttpHost(dest.split(":")[0], Integer.valueOf(dest.split(":")[1]));

                System.out.println("Enter throughput:");
                int throughput = s.nextInt();

                System.out.println("Assync? y/n");
                boolean asynchronous = s.nextLine().equals("y");

                System.out.println("Measure data received? y/n");
                boolean measureDataReceived = s.nextLine().equals("y");

                System.out.println("Log to disk? y/n");
                boolean logToDisk = s.nextLine().equals("y");

                ClientConfiguration config = new ClientConfiguration(target, throughput, asynchronous, logToDisk, measureDataReceived);


                switch (cmd.charAt(0)) {
                case 'a':
                    // Send the request list using puppet
                    List<AppRequest> story = newStory(s, config);
                    if (story == null)
                        continue;
                    System.out.println("Enter the number of clients per node: ");
                    int numberOfClients = s.nextInt();
                    master.newRequests(story, config, numberOfClients);
                    break;
                case 'b':
                    System.out.println("Enter the list of filenames splited by commas:");
                    List<File> files = new ArrayList<File>();
                    for (String fName : Arrays.asList(s.next().split(","))) {
                        File f = new File(BASE_DIR + fName);
                        files.add(f);
                    }
                    if (files.isEmpty()) {
                        throw new Exception("No file found");
                    }
                    System.out.println("Enter number of lines or -1 to all: ");
                    int numberOfLines = s.nextInt();
                    System.out.println("Enter the read percentage [0,1]: ");
                    double readPercentage = s.nextDouble();
                    System.out.println("Is the file ordered per topic? y/n");
                    boolean perTopic = (s.next().toCharArray()[0] == 'y');
                    master.newFile(files, config, numberOfLines, readPercentage, perTopic);
                    break;
                case 'c':
                    master.startExec();
                    break;
                default:
                    System.out.println("Invalid option");
                }
            } catch (Exception e) {
                log.error(e);
            }
        }

    }

    private List<AppRequest> newStory(Scanner s, ClientConfiguration config) {
        List<AppRequest> requests = new LinkedList<AppRequest>();

        String randomText = getRandomText();

        AppRequest.Builder req;
        while (true) {
            System.out.println("a) New Question");
            System.out.println("b) New Answer");
            System.out.println("c) New Comment");
            System.out.println("e) End");

            char cmd = s.next().charAt(0);
            if (cmd == 'e')
                break;

            System.out.println("Number of times: ");
            int nExec = s.nextInt();
            switch (cmd) {
            case 'a':
                lastTitle = getTitle();
                req = bufferCreator.postNewQuestion(SERVER_URL, lastTitle, tags, randomText, AUTHOR, "1", "1", null).setNExec(nExec);
                System.out.println("New question");
                break;
            case 'b':
                req = bufferCreator.postAnswer(SERVER_URL, lastTitle, randomText, AUTHOR, null).setNExec(nExec);
                lastAnswerId = bufferCreator.generateAnswerId(lastTitle, AUTHOR, randomText);
                System.out.println("New answer");
                break;
            case 'c':
                req = bufferCreator.postComment(SERVER_URL, lastTitle, lastAnswerId, randomText, AUTHOR).setNExec(nExec);
                System.out.println("New comment");
                break;
            case 'd':
                System.out.println("Enter the uri (e.g.: /) : ");
                String uri = s.next();
                req = bufferCreator.newGet(uri);
                req.setNExec(nExec);
            default:
                return null;
            }
            requests.add(req.build());
        }
        return requests;
    }

    private String getRandomText() {
        String txt = loremIpsum.getWords(RANDOM_TEXT_SIZE, lorumPosition);
        lorumPosition += RANDOM_TEXT_SIZE;
        return txt;
    }


    @SuppressWarnings("unused")
    private List<InetSocketAddress> selectNodes(Scanner s) {
        System.out.println("Available nodes:");
        int i = 0;
        List<InetSocketAddress> slaves = master.slaves;
        for (InetSocketAddress uri : slaves) {
            System.out.println(i + "->" + uri.getHostString() + ":" + uri.getPort());
        }
        System.out.println("Enter empty for all or the list like: 1,3,4");
        String nodesStr = s.next();
        if (nodesStr.equals("")) {
            return slaves;
        }
        String[] nIndexs = nodesStr.split(",");
        List<InetSocketAddress> newList = new ArrayList<InetSocketAddress>();
        for (String index : nIndexs) {
            i = Integer.parseInt(index);
            newList.add(slaves.get(i));
        }
        return newList;
    }

    public String getTitle() {
        SimpleDateFormat f = new SimpleDateFormat();
        f.applyPattern("hh:mm:ss dd/MM/yyyy");
        SimpleDateFormat fhm = new SimpleDateFormat();
        fhm.applyPattern("hh_mm_ss");
        return fhm.format(new Date());
    }
}
