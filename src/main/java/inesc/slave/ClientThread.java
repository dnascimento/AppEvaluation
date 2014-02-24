package inesc.slave;

import inesc.slave.reports.ThreadReport;

import org.apache.http.HttpEntity;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

/**
 * A thread that performs a GET.
 */
class ClientThread extends
        Thread {
    private static Logger log = Logger.getLogger(ClientThread.class);

    private final CloseableHttpClient httpClient;
    private final HttpContext context;
    private final int clientID;
    private HttpRequestBase[] history;
    private short[] historyCounter;
    private final short[] executionTimes;
    public ThreadReport report;
    private final ClientManager clientManager;
    private long dataReceived;


    public ClientThread(CloseableHttpClient httpClient,
            HttpRequestBase[] history,
            short[] historyCounter,
            int clientID,
            ClientManager clientManager) {
        this.httpClient = httpClient;
        this.clientManager = clientManager;
        this.history = history;
        this.historyCounter = historyCounter;
        this.clientID = clientID;



        context = new BasicHttpContext();
        report = new ThreadReport();
        int totalRequests = report.preExecution(historyCounter, clientID);
        executionTimes = new short[totalRequests];
        dataReceived = 0;
    }



    /**
     * Executes the GetMethod and prints some status information.
     */
    @Override
    public void run() {
        log.info("Client" + clientID + "starting...");
        long startExecution = System.currentTimeMillis();
        int requestID = 0;
        long start;
        long duration;

        for (int i = 0; i < history.length; i++) {
            HttpRequestBase req = history[i];

            while (historyCounter[i]-- > 0) {
                CloseableHttpResponse response;
                try {
                    start = System.currentTimeMillis();
                    response = httpClient.execute(req, context);
                    duration = (System.currentTimeMillis() - start);
                    executionTimes[requestID] = (short) duration;
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        byte[] bytes = EntityUtils.toByteArray(entity);
                        dataReceived += bytes.length;
                    }
                } catch (NoHttpResponseException e) {
                    // Server received the request but due to overload do not reply
                    log.warn("No HTTP Response");
                    executionTimes[requestID] = -1;

                } catch (ConnectionPoolTimeoutException e) {
                    // multithreaded connection manager fails to obtain a free connection
                    log.warn("Connection Pool Timeout Exception");
                    executionTimes[requestID] = -2;

                } catch (ConnectTimeoutException e) {
                    // unable to establish a connection
                    log.warn("Connect Timeout Exception");
                    executionTimes[requestID] = -3;

                } catch (Exception e) {
                    log.warn(e);
                    executionTimes[requestID] = -4;
                }

                // Delay the next request
                try {
                    sleep(ClientManager.DELAY_BETWEEN_REQUESTS);
                } catch (InterruptedException e) {
                    log.error(e);
                }
                requestID++;
            }
        }
        long totalExecutionTime = System.currentTimeMillis() - startExecution;
        String reportString = "";
        report.afterExecution(executionTimes,
                              totalExecutionTime,
                              reportString,
                              dataReceived);
        clientManager.addReport(clientID, report);
        // Free Memory
        history = null;
        historyCounter = null;
    }
}
