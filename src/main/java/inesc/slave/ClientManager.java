package inesc.slave;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.Logger;

public class ClientManager extends
        Thread {
    private static Logger log = Logger.getLogger(ClientManager.class);

    /* Max number of concurrent threads */
    public static final int MAX_CONNECTIONS_TOTAL = 200;
    /* Max number of concurrent threads using same route */
    public static final int MAX_CONNECTIONS_PER_ROUTE = 20;

    /* milisecounds delay */
    public static final int DELAY_BETWEEN_REQUESTS = 100;

    private final LinkedList<ClientThread> clientThreads = new LinkedList<ClientThread>();

    private final CloseableHttpClient httpClient;
    private int id = 0;

    public ClientManager() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(MAX_CONNECTIONS_TOTAL);
        cm.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);

        httpClient = HttpClients.custom().setConnectionManager(cm).build();
    }

    public void newClient(HttpRequestBase[] history, int[] historyCounter) {
        ClientThread thread = new ClientThread(httpClient, history, historyCounter, id++);
        clientThreads.add(thread);
        log.info("New Client with " + history.length + " requests");
    }

    /**
     * Start all clients at same time
     */
    @Override
    public void start() {
        log.info("Starting Clients....");

        // start the threads
        for (ClientThread thread : clientThreads) {
            log.info("TRY TO START");
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
        // TODO Notificar o servidor com os resultados
    }


}
