package inesc.slave;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
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
    private final int id;
    private HttpRequestBase[] history;
    private int[] historyCounter;

    public ClientThread(CloseableHttpClient httpClient,
            HttpRequestBase[] history,
            int[] historyCounter,
            int id) {
        this.httpClient = httpClient;
        context = new BasicHttpContext();
        this.history = history;
        this.historyCounter = historyCounter;
        this.id = id;
    }



    /**
     * Executes the GetMethod and prints some status information.
     */
    @Override
    public void run() {
        StringBuilder report = new StringBuilder();
        log.info("Client" + id + "starting...");
        report.append("Client: " + id + "\n");

        for (int i = 0; i < history.length; i++) {
            HttpRequestBase req = history[i];

            report.append(req.getMethod());
            report.append("-");
            report.append(req.getURI());

            while (historyCounter[i]-- > 0) {
                try {
                    long start = System.currentTimeMillis();
                    CloseableHttpResponse response = httpClient.execute(req, context);
                    long duration = (System.currentTimeMillis() - start);
                    try {
                        report.append(" - ");
                        report.append(duration);
                        HttpEntity entity = response.getEntity();
                        if (entity != null) {
                            byte[] bytes = EntityUtils.toByteArray(entity);
                            report.append(" : ");
                            report.append(bytes.length);
                        }
                    } finally {
                        response.close();
                    }
                    report.append("/");
                    // Delay the next request
                    sleep(ClientManager.DELAY_BETWEEN_REQUESTS);
                } catch (Exception e) {
                    report.append(e);
                }
            }
            report.append("\n");
        }
        log.info(report.toString());
        // Free Memory
        history = null;
        historyCounter = null;
    }
}
