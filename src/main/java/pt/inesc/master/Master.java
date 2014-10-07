/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */
package pt.inesc.master;

import inesc.shared.AppEvaluationProtos.AppAck;
import inesc.shared.AppEvaluationProtos.AppAck.ResStatus;
import inesc.shared.AppEvaluationProtos.FileMsg;
import inesc.shared.AppEvaluationProtos.FromMaster;
import inesc.shared.AppEvaluationProtos.HistoryMsg;
import inesc.shared.AppEvaluationProtos.HistoryMsg.AppRequest;
import inesc.shared.AppEvaluationProtos.ToMaster;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import pt.inesc.slave.Slave;
import pt.inesc.slave.clients.ClientConfiguration;
import pt.inesc.slave.clients.Report;

/**
 * Responsible for:
 * - Track existing slaves
 * - Send requests to every slave
 * - Collect responses
 * 
 * @author darionascimento
 */
public class Master {
    public static final int MASTER_PORT = 9999;


    LinkedList<InetSocketAddress> slaves = new LinkedList<InetSocketAddress>();
    private static Logger log = Logger.getLogger(Master.class);
    private static final String BASE_DIR = "master/";
    private static final LinkedList<Report> reports = new LinkedList<Report>();
    private static int K = 1024;
    private final InetSocketAddress master_address;
    private final MasterService service;


    public Master(List<String> slaves) throws Exception {
        this(slaves, getAddress());
    }


    /**
     * how many slaves should registry before start actions
     * 
     * @param slaves
     */

    public Master(List<String> slaves, String hostname) throws Exception {
        master_address = new InetSocketAddress(hostname, MASTER_PORT);
        System.out.println("Starting Master " + master_address.getHostName() + ":" + master_address.getPort() + "....");

        for (String slAddr : slaves) {
            this.slaves.add(new InetSocketAddress(slAddr, Slave.SLAVE_PORT));
        }

        service = new MasterService(this);
        service.start();
    }







    public static void main(String[] args) throws Exception {
        DOMConfigurator.configure("log4j.xml");
        List<String> slaves = new ArrayList<String>();
        slaves.add("localhost");
        Master master;

        new Slave("localhost");

        if (args.length == 0) {
            master = new Master(slaves);
        } else {
            master = new Master(slaves, args[0]);
        }

        new Interface(master).start();



        log.info("Hit to stop...");
        System.in.read();
    }


    private void sendFile(String filename, Socket s) throws Exception {
        // read file
        FileReader fr = null;
        try {
            File f = new File(BASE_DIR + filename);
            long fileSize = f.length();
            fr = new FileReader(f);
            OutputStreamWriter out = new OutputStreamWriter(s.getOutputStream());
            int readed;
            int sum = 0;
            char[] buffer = new char[1024 * K];
            while ((readed = fr.read(buffer)) > 0) {
                sum += readed;
                log.info((int) ((double) sum / fileSize * 100) + " %");
                out.write(buffer, 0, readed);
            }
            out.flush();
            log.info("100 %");
            out.close();
        } catch (Exception e) {
            log.error(e);
        } finally {
            try {
                fr.close();
            } catch (Exception e) {

            }
        }

    }

    /**
     * New report from slave
     * 
     * @param report
     * @param string
     */
    public void addReport(Report report, String string) {
        System.out.println("New report from host: " + string);
        reports.add(report);
        System.out.println(report);
        synchronized (reports) {
            reports.notify();
        }
    }

    /**
     * Get the reports collected until now
     * 
     * @return
     */
    public String getReports() {
        StringBuilder sb = new StringBuilder();
        for (Report msg : reports) {
            sb.append(msg);
            sb.append("-----------------------------------------\n");
        }
        return sb.toString();
    }










    public void newFile(List<File> fileNames, ClientConfiguration config, Integer numberOfLines, double readPercentage, boolean perTopic) throws Exception {
        FileMsg.Builder fileMsg = FileMsg.newBuilder()
                                         .setConfiguration(config.toProtoBuf())
                                         .setNumberOfLines(numberOfLines)
                                         .setReadPercentage(readPercentage)
                                         .setPerTopic(perTopic);
        for (File f : fileNames) {
            fileMsg.addFilename(f.getName());
        }

        for (InetSocketAddress nodeAddress : slaves) {
            Socket s = new Socket(nodeAddress.getAddress(), nodeAddress.getPort());
            // send filename
            FromMaster msg = FromMaster.newBuilder().setMasterHost(master_address.getHostName()).setFileMsg(fileMsg).build();
            msg.writeDelimitedTo(s.getOutputStream());

            // get ack and send file if required
            AppAck ack = ToMaster.parseDelimitedFrom(s.getInputStream()).getAckMsg();
            if (ack.getStatus().equals(ResStatus.ERROR)) {
                log.info("File does not exist in client, transfering...");
                sendFile(ack.getText(), s);
            }
            s.close();
        }
    }

    public void newRequests(List<AppRequest> requests, ClientConfiguration config, int numberOfClients) throws IOException {
        HistoryMsg story = HistoryMsg.newBuilder()
                                     .addAllRequests(requests)
                                     .setNClients(numberOfClients)
                                     .setConfiguration(config.toProtoBuf())
                                     .build();

        // Send to every slave
        for (InetSocketAddress nodeAddress : slaves) {
            Socket s = new Socket(nodeAddress.getAddress(), nodeAddress.getPort());
            FromMaster msg = FromMaster.newBuilder().setMasterHost(master_address.getHostName()).setHistoryMsg(story).build();
            msg.writeDelimitedTo(s.getOutputStream());
            s.close();
        }
    }

    /**
     * Order slaves to start the request
     * 
     * @return one report per slave
     * @throws InterruptedException
     */
    public LinkedList<Report> startExec() throws InterruptedException {
        reports.clear();

        for (InetSocketAddress nodeAddress : slaves) {
            try {
                log.info("Ordering node" + nodeAddress.getHostString() + ":" + nodeAddress.getPort() + " to start...");
                Socket s = new Socket(nodeAddress.getAddress(), nodeAddress.getPort());
                FromMaster msg = FromMaster.newBuilder().setMasterHost(master_address.getHostName()).setStart(true).build();
                msg.writeDelimitedTo(s.getOutputStream());
                s.close();
            } catch (UnknownHostException e) {
                log.error("Connection refused " + nodeAddress, e);
            } catch (IOException e) {
                log.error("Connection refused " + nodeAddress, e);
            }
        }
        log.info("All nodes started!");

        // Now, wait for the reports:
        while (reports.size() < slaves.size()) {
            synchronized (reports) {
                reports.wait();
            }
        }
        return reports;
    }




    public static String getAddress() throws Exception {
        // 3 cases: only 127.0 , only a non 127 or various
        List<String> validIp = new ArrayList<String>();

        Pattern pattern = Pattern.compile("\\d*\\.\\d*\\.\\d*\\.\\d*");


        Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
        for (; n.hasMoreElements();) {
            NetworkInterface e = n.nextElement();

            Enumeration<InetAddress> a = e.getInetAddresses();
            for (; a.hasMoreElements();) {
                String addr = a.nextElement().getHostAddress();
                Matcher m = pattern.matcher(addr);
                if (m.matches()) {
                    validIp.add(addr);
                }

            }
        }
        switch (validIp.size()) {
        case 0:
            throw new Exception("No ip v4 founded");
        case 1:
            return validIp.get(0);
        case 2:
            validIp.remove("127.0.0.1");
            return validIp.get(0);
        default:
            validIp.remove("127.0.0.1");
            System.out.println("Select interface: ");
            for (int i = 0; i < validIp.size(); i++) {
                System.out.println(i + ")" + validIp.get(i));
            }
            Scanner s = new Scanner(System.in);
            int option = s.nextInt();
            s.close();
            return validIp.get(option);
        }
    }


    public void stop() {
        service.end();
    }
}
