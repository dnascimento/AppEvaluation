package inesc;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

/**
 * A thread that performs a GET.
 */
class GetThread extends
        Thread {

    private final CloseableHttpClient httpClient;
    private final HttpContext context;
    private final HttpGet httpget;
    private final int id;
    private int replays;
    private int delay;

    public GetThread(CloseableHttpClient httpClient,
            HttpGet httpget,
            int replays,
            int delay,
            int id) {
        this.httpClient = httpClient;
        context = new BasicHttpContext();
        this.httpget = httpget;
        this.replays = replays;
        this.delay = delay;
        this.id = id;
    }

    /**
     * Executes the GetMethod and prints some status information.
     */
    @Override
    public void run() {
        while (replays-- > 0) {

            try {
                System.out.println(id + " - about to get something from "
                        + httpget.getURI());
                long start = System.nanoTime();
                CloseableHttpResponse response = httpClient.execute(httpget, context);
                long duration = (System.nanoTime() - start) / 1000000;

                try {
                    System.out.println(id + " - get executed in: " + duration);
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        byte[] bytes = EntityUtils.toByteArray(entity);
                        System.out.println(id + " - " + bytes.length + " bytes read");
                    }
                } finally {
                    response.close();
                }
                // Delay the next request
                sleep(delay);
            } catch (Exception e) {
                System.out.println(id + " - error: " + e);
            }
        }
    }
}
