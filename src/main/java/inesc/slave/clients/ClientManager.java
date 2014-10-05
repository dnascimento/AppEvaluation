/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */

package inesc.slave.clients;


import inesc.slave.Slave;

import java.io.File;
import java.util.LinkedList;

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


    private final LinkedList<ClientThread> clientThreads = new LinkedList<ClientThread>();
    private ThreadReport[] clientReports;
    private int id;
    public final Slave slave;


    public ClientManager(Slave slave) {
        restart();
        this.slave = slave;
    }

    public void restart() {
        // TODO
        clientThreads.clear();
        clientReports = null;
        id = 0;
    }

    public void newFile(File f, ClientConfiguration config) {
        ClientThread thread = new ClientThreadFileBased(id++, this, f, config);
        clientThreads.add(thread);
        log.info("New Client using file " + f);
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


    public void runSync() {
        clientReports = new ThreadReport[id];
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

        if (slave.masterIsAvailable) {
            slave.sendReportToMaster(clientReports);
        } else {
            for (ThreadReport report : clientReports) {
                System.out.println(report);
            }
        }

        // Clean the threads and connections
        this.restart();
    }


    /**
     * Add report after execution (invoked per thread)
     * 
     * @param clientId
     * @param report
     */
    public synchronized void addReport(int clientId, ThreadReport report) {
        clientReports[clientId] = report;
    }


    public ThreadReport[] getReports() {
        return clientReports;
    }

}
