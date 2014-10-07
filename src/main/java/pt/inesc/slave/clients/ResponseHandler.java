package pt.inesc.slave.clients;

import java.io.IOException;

import org.apache.http.ContentTooLongException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ContentBufferEntity;
import org.apache.http.nio.protocol.AbstractAsyncResponseConsumer;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.nio.util.SimpleInputBuffer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Asserts;

public class ResponseHandler extends
        AbstractAsyncResponseConsumer<HttpResponse> {

    protected volatile HttpResponse response;
    /** Bytes received counter */
    protected volatile Statistics stats;

    private volatile SimpleInputBuffer buf;


    public ResponseHandler(Statistics stats) {
        this.stats = stats;
    }

    /**
     * Invoked when a HTTP response message is received.
     */
    @Override
    protected void onResponseReceived(HttpResponse response) throws IOException {
        this.response = response;
    }

    /**
     * Invoked if the response message encloses a content entity.
     */
    @Override
    protected void onEntityEnclosed(final HttpEntity entity, final ContentType contentType) throws IOException {
        long len = entity.getContentLength();
        if (len > Integer.MAX_VALUE) {
            throw new ContentTooLongException("Entity content is too long: " + len);
        }
        if (len < 0) {
            len = 4096;
        }
        this.buf = new SimpleInputBuffer((int) len, new HeapByteBufferAllocator());
        this.response.setEntity(new ContentBufferEntity(entity, this.buf));
    }

    /**
     * Invoked to process a chunk of content from the ContentDecoder.
     */
    @Override
    protected void onContentReceived(ContentDecoder decoder, IOControl ioctrl) throws IOException {
        Asserts.notNull(this.buf, "Content buffer");
        int bytesReaded = this.buf.consumeContent(decoder);
        stats.newResponse(bytesReaded, false);
    }


    /**
     * Invoked to release all system resources currently allocated.
     */
    @Override
    protected void releaseResources() {
        response = null;
        this.buf = null;
        this.stats = null;

    }

    /**
     * Invoked to generate a result object from the received HTTP response message.
     */
    @Override
    protected HttpResponse buildResult(HttpContext context) {
        return this.response;
    }
}
