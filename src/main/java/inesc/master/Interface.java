/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */

package inesc.master;


import inesc.master.commands.AskIntBuffers;
import inesc.master.commands.AskRequestStory;
import inesc.shared.AppEvaluationProtos.AppReqList;
import inesc.shared.AppEvaluationProtos.AppRequest;
import inesc.shared.AppEvaluationProtos.AppStartMsg.StartOpt;

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

                switch (cmd.charAt(0)) {
                case 'a':
                    // Send the request list using puppet
                    AppReqList story = newStory(s);
                    if (story == null)
                        continue;
                    nodes = selectNodes(s);
                    master.sendRequest(story, nodes);
                    break;
                case 'b':
                    System.out.println("Enter the file name");
                    String filename = s.next();
                    nodes = selectNodes(s);
                    master.sendFileName(filename, nodes, SERVER_URL);
                    break;
                case 'c':
                    master.start(StartOpt.Disk);
                    break;
                }
            } catch (Exception e) {
                log.error(e);
            }
        }

    }

    private AppReqList newStory(Scanner s) {
        System.out.println("How many clients per node?");
        int nNodes = s.nextInt();
        String randomText = getRandomText();
        AppRequest req;
        AskRequestStory story = new AskRequestStory(master);
        while (true) {
            System.out.println("a) New Question");
            System.out.println("b) New Answer");
            System.out.println("c) New Comment");
            System.out.println("e) End");

            String cmd = s.next();
            switch (cmd.charAt(0)) {
            case 'a':
                lastTitle = getTitle();
                req = bufferCreator.postNewQuestion(SERVER_URL, lastTitle, tags, randomText, AUTHOR, "1", "1").setNExec(nNodes).build();
                System.out.println("New question");
                break;
            case 'b':
                req = bufferCreator.postAnswer(SERVER_URL, lastTitle, randomText, AUTHOR).setNExec(nNodes).build();
                lastAnswerId = bufferCreator.generateAnswerId(lastTitle, AUTHOR, randomText);
                System.out.println("New answer");
                break;
            case 'c':
                req = bufferCreator.postComment(SERVER_URL, lastTitle, lastAnswerId, randomText, AUTHOR).setNExec(nNodes).build();
                System.out.println("New comment");
                break;
            case 'e':
                return story.build(nNodes);
            default:
                return null;
            }
            story.addRequest(req);
        }
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
