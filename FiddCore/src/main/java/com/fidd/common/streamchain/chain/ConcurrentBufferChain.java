package com.fidd.common.streamchain.chain;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ConcurrentBufferChain implements BufferChain {
    protected volatile @Nullable BufferFlusher flusher;
    protected volatile boolean closed;
    protected final ConcurrentLinkedQueue<byte[]> queue;

    public ConcurrentBufferChain() {
        this.closed = false;
        this.queue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void addBuffer(byte[] buffer) {
        queue.add(buffer);
    }

    @Override
    public byte[] pollBuffer() {
        byte[] nextBuffer = queue.poll();
        if (nextBuffer == null) {
            if (flusher != null) {
                flusher.flushBuffer();
                nextBuffer = queue.poll();
            }
        }
        return nextBuffer;
    }

    /** Approximation */
    @Override
    public long available() {
        if (queue.isEmpty()) {
            if (flusher != null) { flusher.flushBuffer(); }
        }

        long available = 0;
        for (byte[] buffer : queue) {
            available += buffer.length;
        }

        return available;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        this.closed = true;
    }

    @Override
    public void attachBufferFlusher(BufferFlusher flusher) {
        this.flusher = flusher;
    }
}
