package pt.inesc.slave.clients;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.client.methods.HttpRequestBase;

public class ClientThreadRequestBased extends
        ClientThread {

    /** Set of requests to perform **/
    private HttpRequestBase[] history;

    /** How many times each request is performed */
    private long[] historyCounter;
    private final AtomicBoolean stop = new AtomicBoolean(false);

    public ClientThreadRequestBased(HttpRequestBase[] history,
            long[] counter,
            int clientID,
            ClientManager clientManager,
            ClientConfiguration config) {
        super(clientID, clientManager, config);
        this.history = history;
        this.historyCounter = counter;
    }

    /**
     * Executes the GetMethod and prints status information.
     */
    @Override
    public void run() {
        log.info("Client" + clientId + "starting...");
        Thread.currentThread().setName("RequestBased Thread " + clientId);
        for (int i = 0; i < history.length; i++) {
            if (stop.get()) {
                break;
            }
            HttpRequestBase req = history[i];
            while (historyCounter[i]-- > 0) {
                execRequest(req);
            }
        }
        try {
            end();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Free Memory (GB Collect later)
        history = null;
        historyCounter = null;
    }

    @Override
    public void over() {
        stop.set(true);
    }

}
