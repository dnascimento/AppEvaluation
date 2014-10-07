package pt.inesc.slave.clients;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;

public class ResponseHandlerFlush extends
        ResponseHandler {


    /** File to flush the responses */
    private final FileChannel out;


    public ResponseHandlerFlush(Statistics stats, FileOutputStream out) {
        super(stats);
        this.out = out.getChannel();
    }

    /**
     * Invoked to process a chunk of content from the ContentDecoder.
     */
    @Override
    protected void onContentReceived(ContentDecoder decoder, IOControl ioctrl) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        int bytesReaded = 0;
        while ((bytesReaded = decoder.read(buffer)) != 0) {
            buffer.flip();
            out.write(buffer);
            buffer.clear();
            stats.newResponse(bytesReaded, false);
        }
    }
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
