package inesc.slave.clients;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;

public class ClientThreadRequestBased extends
        ClientThread {

    /** Set of requests to perform **/
    private HttpRequestBase[] history;

    /** How many times each request is performed */
    private short[] historyCounter;


    public ClientThreadRequestBased(CloseableHttpClient httpClient,
            HttpRequestBase[] history,
            short[] historyCounter,
            int clientID,
            ClientManager clientManager) {
        super(httpClient, clientID, clientManager);

        this.history = history;
        this.historyCounter = historyCounter;
        for (int i = 0; i < historyCounter.length; i++) {
            totalRequests += historyCounter[i];
        }

        executionTimes = new ArrayList<Short>((int) totalRequests);
        responseData = new ArrayList<ByteBuffer>((int) totalRequests);
    }

    /**
     * Executes the GetMethod and prints status information.
     */
    @Override
    public void run() {
        initStatistics();
        log.info("Client" + clientID + "starting...");
        for (int i = 0; i < history.length; i++) {
            HttpRequestBase req = history[i];

            while (historyCounter[i]-- > 0) {
                execRequest(req);
                // Delay the next request
                try {
                    sleep(ClientManager.DELAY_BETWEEN_REQUESTS);
                } catch (InterruptedException e) {
                    log.error(e);
                }
            }
        }
        collectStatistics();
        // Free Memory (GB Collect later)
        history = null;
        historyCounter = null;
    }


}
