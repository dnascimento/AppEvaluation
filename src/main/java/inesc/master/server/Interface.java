/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */

package inesc.master.server;

import inesc.master.AskInterface;
import inesc.master.AskRequestHistory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class Interface extends
        Thread {
    List<String> cmds = createCmdList("start requests", "new question", "new answer", "new comment");
    private final Master master;
    AskRequestHistory history = new AskRequestHistory();
    String lastTitle;

    public Interface(Master master) {
        this.master = master;
    }

    @Override
    public void run() {
        @SuppressWarnings("resource")
        Scanner s = new Scanner(System.in);
        SimpleDateFormat f = new SimpleDateFormat();
        f.applyPattern("hh:mm:ss dd/MM/yyyy");
        SimpleDateFormat fhm = new SimpleDateFormat();
        fhm.applyPattern("mm_ss");
        while (true) {
            String randHm = fhm.format(new Date());
            String randDate = f.format(new Date());
            showCmds();
            char[] optString = s.next().toCharArray();
            switch (optString[0]) {
            case 'a':
                startRequests();
                break;
            case 'b':
                lastTitle = "t" + randHm;
                history.addRequest(AskInterface.postNewQuestion(1, "t" + randHm, "ist", randDate));
                break;
            case 'c':
                if (lastTitle == null) {
                    System.out.println("LastTitle Empty");
                    break;
                }
                history.addRequest(AskInterface.postAnswer(1, lastTitle, randDate));
                break;
            default:
                break;
            }
        }
    }

    private void startRequests() {
        history.start();
        history = new AskRequestHistory();
    }




    private void showCmds() {
        int i = 0;
        for (String cmd : cmds) {
            System.out.print((char) ('a' + i++));
            System.out.print(") ");
            System.out.print(cmd);
            System.out.print("\n");
        }
    }

    private List<String> createCmdList(String... cmds) {
        ArrayList<String> list = new ArrayList<String>();
        for (String s : cmds) {
            list.add(s);
        }
        return list;
    }
}
