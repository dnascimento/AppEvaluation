package pt.inesc.slave.clients;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
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

    @Override
    public void completed(HttpResponse response) {
        latch.countDown();
        long duration = (System.nanoTime() - start);
        StatusLine status = response.getStatusLine();
        boolean ok = (status.getStatusCode() == 200);
        if (!ok) {
            System.out.println(request.getRequestLine() + "->" + status.getStatusCode() + " " + status.getReasonPhrase());
        }
        stats.requestCompleted(duration, ok);


    }

    @Override
    public void cancelled() {
        latch.countDown();
        long duration = (System.nanoTime() - start);
        System.out.println(request.getRequestLine() + " cancelled");
        stats.requestCancelled(duration);
    }


    @Override
    public void failed(Exception ex) {
        latch.countDown();
        ex.printStackTrace();
        long duration = (System.nanoTime() - start);
        System.out.println(request.getRequestLine() + "->" + ex);
        stats.requestFailed(duration);
    }


    // HttpEntity entity = response.getEntity();
    // try {
    // InputStream in = entity.getContent();
    //
    // BufferedOutputStream out = new BufferedOutputStream(System.out);
    // byte[] b = new byte[1024];
    // while ((in.read(b)) != 0) {
    // out.write(b);
    // }
    // out.close();
    // in.close();
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
}
