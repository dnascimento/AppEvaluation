package inesc.slave.clients;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;

public class FutureEvalCallback
        implements FutureCallback<HttpResponse> {

    private final CountDown latch;
    private final HttpRequest request;
    private final long start;
    private final Statistics stats;


    public FutureEvalCallback(long start, CountDown latch, HttpRequest request, Statistics stats) {
        this.latch = latch;
        this.request = request;
        this.start = start;
        this.stats = stats;
    }

    public void completed(HttpResponse response) {
        latch.countDown();
        long duration = (System.nanoTime() - start);
        // System.out.println(request.getRequestLine() + "->" + response.getStatusLine());
        stats.requestCompleted(duration);

    }

    public void cancelled() {
        latch.countDown();
        long duration = (System.nanoTime() - start);
        // System.out.println(request.getRequestLine() + " cancelled");
        stats.requestCancelled(duration);
    }


    public void failed(Exception ex) {
        latch.countDown();
        long duration = (System.nanoTime() - start);
        // System.out.println(request.getRequestLine() + "->" + ex);
        stats.requestFailed(duration);
    }
}
