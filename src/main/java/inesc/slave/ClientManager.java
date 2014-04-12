package inesc.slave;


import inesc.shared.AppEvaluationProtos.AppStartMsg.StartOpt;
import inesc.slave.reports.ThreadReport;
import inesc.slave.server.Slave;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
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
    private CloseableHttpClient httpClient;
    private int id;
    private final Slave slave;


    public ClientManager(Slave slave) {
        restart();
        this.slave = slave;
    }

    public void restart() {
        clientThreads.clear();
        clientReports = null;
        /*
         * Perform explicit garbage collection to remove old client threads and reports.
         * System.gc();
         */
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(MAX_CONNECTIONS_TOTAL);
        cm.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);

        RequestConfig defaultRequestConfig = RequestConfig.custom()
                                                          .setSocketTimeout(SOCKET_TIMEOUT)
                                                          .setConnectTimeout(CONNECTION_TIMEOUT)
                                                          .setConnectionRequestTimeout(REQUEST_TIMEOUT)
                                                          .setStaleConnectionCheckEnabled(true)
                                                          .build();

        httpClient = HttpClients.custom()
                                .setConnectionManager(cm)
                                .setDefaultRequestConfig(defaultRequestConfig)
                                .build();
        id = 0;
    }

    public void newClient(HttpRequestBase[] history, short[] historyCounter) {
        ClientThread thread = new ClientThread(httpClient, history, historyCounter, id++, this);
        clientThreads.add(thread);
        log.info("New Client with " + history.length + " requests");
    }

    /**
     * Start all clients at same time
     */
    @Override
    public void run() {
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

        try {
            httpClient.close();
        } catch (IOException e) {
            log.error("Error closing HTTP Client" + e);
        }
        log.info("Clients done...");

        slave.sendReportToMaster(clientReports);
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
}
