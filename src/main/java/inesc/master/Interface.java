/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */

package inesc.master;


import inesc.master.commands.AskIntBuffers;
import inesc.shared.AppEvaluationProtos.Configuration;
import inesc.shared.AppEvaluationProtos.FileMsg;
import inesc.shared.AppEvaluationProtos.HistoryMsg;
import inesc.shared.AppEvaluationProtos.HistoryMsg.AppRequest;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;

import de.svenjacobs.loremipsum.LoremIpsum;

public class Interface extends
        Thread {
    private static final int RANDOM_TEXT_SIZE = 20;
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
        List<InetSocketAddress> nodes;

        while (true) {
            try {
                System.out.println("a) Add new story");
                System.out.println("b) Send file");
                System.out.println("c) Start stories");
                cmd = s.next();

                Configuration.Builder config = Configuration.newBuilder();
                System.out.println("Enter throughput:");
                config.setThroughput(s.nextInt());

                System.out.println("Assync? y/n");
                config.setAssynchronous(s.nextLine().equals("y"));

                System.out.println("Measure data received? y/n");
                config.setMeasureDataReceived(s.nextLine().equals("y"));

                System.out.println("Log to disk? y/n");
                config.setLogToDisk(s.nextLine().equals("y"));

                switch (cmd.charAt(0)) {
                case 'a':
                    // Send the request list using puppet
                    HistoryMsg story = newStory(s, config);
                    if (story == null)
                        continue;
                    nodes = selectNodes(s);
                    master.send(story, nodes);
                    break;
                case 'b':
                    FileMsg.Builder msg = FileMsg.newBuilder();
                    msg.setConfiguration(config);

                    System.out.println("Enter the file name:");
                    msg.setFilename(s.next());

                    nodes = selectNodes(s);
                    master.send(msg.build(), nodes);
                    break;
                case 'c':
                    master.start();
                    break;
                }
            } catch (Exception e) {
                log.error(e);
            }
        }

    }

    private HistoryMsg newStory(Scanner s, Configuration.Builder config) {
        HistoryMsg.Builder msg = HistoryMsg.newBuilder();


        System.out.println("How many clients per node?");
        msg.setNClients(s.nextInt());

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
            default:
                return null;
            }
            msg.addRequests(req);
        }
        return msg.build();
    }

    private String getRandomText() {
        String txt = loremIpsum.getWords(RANDOM_TEXT_SIZE, lorumPosition);
        lorumPosition += RANDOM_TEXT_SIZE;
        return txt;
    }


    private List<InetSocketAddress> selectNodes(Scanner s) {
        System.out.println("Available nodes:");
        int i = 0;
        List<InetSocketAddress> slaves = master.slavelist;
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
