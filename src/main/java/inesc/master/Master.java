/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */
package inesc.master;

import inesc.shared.AppEvaluationProtos.AppAck;
import inesc.shared.AppEvaluationProtos.AppAck.ResStatus;
import inesc.shared.AppEvaluationProtos.FileMsg;
import inesc.shared.AppEvaluationProtos.FromMaster;
import inesc.shared.AppEvaluationProtos.HistoryMsg;
import inesc.shared.AppEvaluationProtos.ReportAgregatedMsg;
import inesc.shared.AppEvaluationProtos.ToMaster;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Responsible for:
 * - Track existing slaves
 * - Send requests to every slave
 * - Collect responses
 * 
 * @author darionascimento
 */
public class Master {
    public static final InetSocketAddress MASTER_ADDRESS = new InetSocketAddress("localhost", 9999);


    LinkedList<InetSocketAddress> slavelist = new LinkedList<InetSocketAddress>();
    private static Logger log = Logger.getLogger(Master.class);
    private static final String BASE_DIR = "master/";
    private static final LinkedList<ReportAgregatedMsg> reports = new LinkedList<ReportAgregatedMsg>();
    private static int K = 1024;
    /** how many slaves should registry before start actions */
    public static final int EXPECTED_SLAVES = 1;

    public Master() throws IOException {
        new Interface(this).start();
        new MasterService(this).start();
    }



    public static void main(String[] args) throws IOException {
        DOMConfigurator.configure("log4j.xml");
        log.info("Starting Master " + MASTER_ADDRESS.getPort() + "....");
        new Master();
        log.info("Waiting for slaves registry..");
        log.info("Hit stop the server...");
        System.in.read();
    }

    public void addNewSlave(InetSocketAddress uri) {
        log.info("New slave registered:" + uri);
        slavelist.add(uri);
        if (slavelist.size() == EXPECTED_SLAVES) {
            log.info("All slaves Registered");
        }
    }


    public void send(HistoryMsg story, List<InetSocketAddress> slaves) throws Exception {
        for (InetSocketAddress nodeAddress : slaves) {
            Socket s = new Socket(nodeAddress.getAddress(), nodeAddress.getPort());
            FromMaster msg = FromMaster.newBuilder().setHistoryMsg(story).build();
            msg.writeDelimitedTo(s.getOutputStream());
            s.close();
        }
    }

    public void send(FileMsg fileMsg, List<InetSocketAddress> nodes) throws Exception {
        for (InetSocketAddress nodeAddress : nodes) {
            Socket s = new Socket(nodeAddress.getAddress(), nodeAddress.getPort());
            // send filename
            FromMaster msg = FromMaster.newBuilder().setFileMsg(fileMsg).build();
            msg.writeDelimitedTo(s.getOutputStream());

            // get ack and send file if required
            AppAck ack = ToMaster.parseDelimitedFrom(s.getInputStream()).getAckMsg();
            if (ack.getStatus().equals(ResStatus.ERROR)) {
                log.info("File does not exist in client, transfering...");
                sendFile(fileMsg.getFilename(), s);
            }
            s.close();
        }
    }

    /**
     * Order slaves to start the request
     * 
     * @param logOptins Record on Disk
     */
    public void start() {
        for (InetSocketAddress nodeAddress : slavelist) {
            try {
                log.info("Ordering node" + nodeAddress.getHostString() + ":" + nodeAddress.getPort() + " to start...");
                Socket s = new Socket(nodeAddress.getAddress(), nodeAddress.getPort());
                FromMaster msg = FromMaster.newBuilder().setStart(true).build();
                msg.writeDelimitedTo(s.getOutputStream());
                s.close();
            } catch (UnknownHostException e) {
                log.error("Connection refused " + nodeAddress, e);
            } catch (IOException e) {
                log.error("Connection refused " + nodeAddress, e);
            }
        }
        log.info("All nodes started!");
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
     * @param reportList
     */
    public void addReport(ReportAgregatedMsg reportList) {
        reports.add(reportList);
        System.out.println(reportList);
    }

    /**
     * Get the reports collected until now
     * 
     * @return
     */
    public String getReports() {
        StringBuilder sb = new StringBuilder();
        for (ReportAgregatedMsg msg : reports) {
            sb.append(msg);
            sb.append("-----------------------------------------\n");
        }
        return sb.toString();
    }


}
