/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */

package inesc.slave.clients;


import inesc.shared.AppEvaluationProtos.AppStartMsg.StartOpt;
import inesc.slave.Slave;

import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.log4j.Logger;

/**
 * Manages the set of ClientThreads. Each thread represents a distinct parallel client
 * 
 * @author darionascimento
 */
public class ClientManager extends
        Thread {
    private static Logger log = Logger.getLogger(ClientManager.class);

    public static final int SOCKET_TIMEOUT = 5000;
    public static final int CONNECTION_TIMEOUT = 5000;
    public static final int REQUEST_TIMEOUT = 5000;

    /* Max number of concurrent threads */
    public static final int MAX_CONNECTIONS_TOTAL = 200;
    /* Max number of concurrent threads using same route */
    public static final int MAX_CONNECTIONS_PER_ROUTE = 200;

    /* milisecounds delay */
    public static final int DELAY_BETWEEN_REQUESTS = 10;

    private final LinkedList<ClientThread> clientThreads = new LinkedList<ClientThread>();
    private ThreadReport[] clientReports;
    private int id;
    public final Slave slave;


    public ClientManager(Slave slave) {
        restart();
        this.slave = slave;
    }

    public void restart() {

        clientThreads.clear();
        clientReports = null;
        id = 0;
    }

    public void newFile(File f, URL targetHost, int throughput) {
        ClientThread thread = new ClientThreadFileBased(id++, this, f, targetHost, throughput);
        clientThreads.add(thread);
        log.info("New Client using file " + f);
    }

    public void newClient(HttpRequestBase[] history, short[] historyCounter, URL targetURL, int throughput) {
        ClientThread thread = new ClientThreadRequestBased(history, historyCounter, id++, this, targetURL, throughput);
        clientThreads.add(thread);
        log.info("New Client with " + history.length + " requests");
    }

    /**
     * Start all clients at same time
     */
    @Override
    public void run() {
        runSync();
    }


    public void runSync() {
        clientReports = new ThreadReport[id];
        log.info("Starting " + clientThreads.size() + "Clients....");

        // start the threads
        for (ClientThread thread : clientThreads) {
            thread.start();
        }

        // join the threads
        for (ClientThread thread : clientThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                log.error("Interrupted Execution" + e);
            }
        }

        log.info("Clients done...");

        if (slave != null)
            slave.sendReportToMaster(clientReports);
        else
            System.out.println(clientReports);
        // Clean the threads and connections
        this.restart();
    }


    /**
     * Add report after execution (invoked per thread)
     * Synchonization is done by individual array access
     * 
     * @param clientId
     * @param report
     */
    public void addReport(int clientId, ThreadReport report) {
        clientReports[clientId] = report;
    }


    /** Set the client Thead Execution Options */
    public void setStartOptions(List<StartOpt> optList) {
        for (ClientThread thread : clientThreads) {
            thread.setStartOptions(optList);
        }
    }

    public ThreadReport[] getReports() {
        return clientReports;
    }

}
