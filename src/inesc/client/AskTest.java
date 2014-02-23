package inesc;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * @author darionascimento
 */
public class AskTest {
    /* Number of threads */
    private static final int N_CLIENTS = 1;
    /* each client performs N_REPLAY times the same request */
    private static final int N_REPLAY = 1;
    /* milisecounds delay */
    private static final int DELAY_BETWEEN = 100;

    public static void main(String[] args) throws Exception {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(N_CLIENTS);

        CloseableHttpClient httpclient = HttpClients.custom()
                                                    .setConnectionManager(cm)
                                                    .build();
        String server = "http://localhost:8888";

        try {
            // create a thread for each URI
            GetThread[] threads = new GetThread[N_CLIENTS];
            for (int i = 0; i < threads.length; i++) {
                // Create the getThread
                HttpGet httpget = new HttpGet(server);
                threads[i] = new GetThread(httpclient, httpget, N_REPLAY, DELAY_BETWEEN,
                        i + 1);
            }

            // start the threads
            for (int j = 0; j < threads.length; j++) {
                threads[j].start();
            }

            // join the threads
            for (int j = 0; j < threads.length; j++) {
                threads[j].join();
            }
        } finally {
            httpclient.close();
        }
        System.out.println("Done");
    }
}
