/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */

package pt.inesc.slave.clients;


import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.log4j.Logger;

import pt.inesc.slave.Slave;

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


    private final LinkedList<ClientThread> clientThreads = new LinkedList<ClientThread>();
    private Statistics[] clientStats;
    private int id;
    public final Slave slave;


    public ClientManager(Slave slave) {
        restart();
        this.slave = slave;
    }

    public void restart() {
        // TODO
        clientThreads.clear();
        clientStats = null;
        id = 0;
    }

    public void newFile(List<File> filesToExec, ClientConfiguration config, Integer numberOfLines, double readPercentage, boolean perTopic) {
        ClientThread thread = new ClientThreadFileBased(id++, this, filesToExec, config, numberOfLines, readPercentage, perTopic);
        clientThreads.add(thread);
        log.info("New Client using file " + filesToExec);
    }

    public void newHistory(HttpRequestBase[] history, long[] counter, ClientConfiguration config) {
        ClientThread thread = new ClientThreadRequestBased(history, counter, id++, this, config);
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


    public Report runSync() {
        clientStats = new Statistics[id];
        log.info("Starting " + clientThreads.size() + " clients....");

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

        Report report = new Report(clientStats, slave.myAddress.getHostName());
        slave.sendReportToMaster(report);

        // Clean the threads and connections
        this.restart();
        return report;
    }


    /**
     * Add report after execution (invoked per thread)
     * 
     * @param clientId
     * @param report
     */
    public synchronized void addStats(int clientId, Statistics stat) {
        clientStats[clientId] = stat;
    }




}
