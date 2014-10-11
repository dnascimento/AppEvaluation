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
}
