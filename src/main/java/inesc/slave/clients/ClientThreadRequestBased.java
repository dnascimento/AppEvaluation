package inesc.slave.clients;

import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.apache.http.client.methods.HttpRequestBase;

public class ClientThreadRequestBased extends
        ClientThread {

    /** Set of requests to perform **/
    private HttpRequestBase[] history;

    /** How many times each request is performed */
    private short[] historyCounter;


    public ClientThreadRequestBased(HttpRequestBase[] history,
            short[] historyCounter,
            int clientID,
            ClientManager clientManager,
            URL hostURL) {
        super(clientID, hostURL, clientManager);

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
