package inesc.slave.clients;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.AbstractAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;

public class ResponseHandler extends
        AbstractAsyncResponseConsumer<HttpResponse> {

    /** Bytes received counter */
    private Statistics stats;

    public ResponseHandler(boolean flush, Statistics stats) {

    }

    /**
     * Invoked to generate a result object from the received HTTP response message.
     */
    @Override
    protected HttpResponse buildResult(HttpContext arg0) throws Exception {
        System.out.println("buildResult");
        return null;
    }

    /**
     * Invoked to process a chunk of content from the ContentDecoder.
     */
    @Override
    protected void onContentReceived(ContentDecoder arg0, IOControl arg1) throws IOException {
        System.out.println("onContentReceived");
        stats.newResponse(0, false);
    }

    /**
     * Invoked if the response message encloses a content entity.
     */
    @Override
    protected void onEntityEnclosed(HttpEntity arg0, ContentType arg1) throws IOException {
        System.out.println("onEntityEnclosed");
    }

    /**
     * Invoked when a HTTP response message is received.
     */
    @Override
    protected void onResponseReceived(HttpResponse arg0) throws HttpException, IOException {
        System.out.println("onResponseReceived");
    }

    /**
     * Invoked to release all system resources currently allocated.
     */
    @Override
    protected void releaseResources() {
        System.out.println("releaseResources");
    }









    // /** Assync log file for flush the responses */
    // private final AsynchronousFileChannel fileChannel = null;
    //
    // /** Flag to flush or not the responses */
    // private final boolean diskLog = false;
    //
    //
    // private final int flushedRequests = 0;
    // private long flushedFilePosition;
    //
    //
    //
    //
    // @Override
    // protected void onCharReceived(CharBuffer buf, IOControl ioctrl) throws IOException
    // {
    // // TODO Auto-generated method stub
    //
    // }
    // initAsyncFile(slavePort);
    // int slavePort = clientManager.slave.myAddress.getPort();
    //
    // @Override
    // protected Boolean buildResult(HttpContext arg0) throws Exception {
    // // TODO Auto-generated method stub
    // return null;
    //
    //
    // ByteBuffer separator = ByteBuffer.wrap("\n--------\n".getBytes());
    // long separatorSize = separator.capacity();
    // responseData = report.responseData;
    // while (flushedRequests < responseData.size() && responseData.get(flushedRequests)
    // != null) {
    //
    // long written = responseData.get(flushedRequests).capacity();
    //
    // separator.rewind();
    // responseData.get(flushedRequests).rewind();
    //
    // fileChannel.write(responseData.get(flushedRequests), flushedFilePosition);
    // flushedFilePosition += written;
    // fileChannel.write(separator, flushedFilePosition);
    // flushedFilePosition += separatorSize;
    // responseData.set(flushedRequests, null);
    // flushedRequests++;
    // }
    //
    // }
    //
    // @Override
    // protected void onResponseReceived(HttpResponse arg0) throws HttpException,
    // IOException {
    // // TODO Auto-generated method stub
    //
    // }
    //
    //
    // private void initAsyncFile(int slavePort) {
    // Path file = Paths.get("responses/" + slavePort + "/" + clientId);
    // try {
    // // create dirs
    // file.toFile().getParentFile().mkdirs();
    // // clean old file
    // file.toFile().delete();
    // // Open the socket to write
    // fileChannel = AsynchronousFileChannel.open(file,
    // StandardOpenOption.CREATE,
    // StandardOpenOption.WRITE,
    // StandardOpenOption.TRUNCATE_EXISTING);
    // } catch (IOException e) {
    // log.error(e);
    // }
    // }
    //
    // // Flush the remain data
    // if (diskLog) {
    // flushData();
    // try {
    // fileChannel.close();
    // } catch (IOException e) {
    // log.error(e);
    // }
    // }
}
