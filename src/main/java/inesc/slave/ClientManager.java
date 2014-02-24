package inesc.slave;


import inesc.slave.reports.ThreadReport;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.Logger;

public class ClientManager extends
        Thread {
    private static Logger log = Logger.getLogger(ClientManager.class);

    public static final int SOCKET_TIMEOUT = 5000;
    public static final int CONNECTION_TIMEOUT = 5000;
    public static final int REQUEST_TIMEOUT = 5000;

    /* Max number of concurrent threads */
    public static final int MAX_CONNECTIONS_TOTAL = 200;
    /* Max number of concurrent threads using same route */
    public static final int MAX_CONNECTIONS_PER_ROUTE = 20;

    /* milisecounds delay */
    public static final int DELAY_BETWEEN_REQUESTS = 10;

    private final LinkedList<ClientThread> clientThreads = new LinkedList<ClientThread>();
    private final HashMap<Integer, ThreadReport> clientReports = new HashMap<Integer, ThreadReport>();
    private CloseableHttpClient httpClient;
    private int id = 0;

    public ClientManager() {
        restart();
    }

    public void restart() {
        clientThreads.clear();
        clientReports.clear();
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
    }

    public void newClient(HttpRequestBase[] history, short[] historyCounter) {
        ClientThread thread = new ClientThread(httpClient, history, historyCounter, id++,
                this);
        clientThreads.add(thread);
        log.info("New Client with " + history.length + " requests");
    }

    /**
     * Start all clients at same time
     */
    @Override
    public void start() {
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
        // HttpConnectionMetrics metris =
        log.info("Clients done...");
        showReports();
        // TODO Notificar o servidor com os resultados
        // Clean the threads and connections
        this.restart();

    }

    private void showReports() {
        for (ThreadReport report : clientReports.values()) {
            log.info(report);
        }

    }

    public synchronized void addReport(int clientId, ThreadReport report) {
        clientReports.put(clientId, report);
    }
}
