package com.fidd.common.streamchain;

import com.fidd.common.streamchain.chain.OutputBufferChain;

import javax.annotation.Nullable;
import java.io.OutputStream;

public class BufferChainOutputStream extends OutputStream implements OutputBufferChain.BufferFlusher {

    private final OutputBufferChain chain;
    private final int bufferSize;
    private @Nullable Long dataLimitBytes;

    private final byte[] buffer;
    private int position;

    private boolean closed = false;

    public BufferChainOutputStream(OutputBufferChain chain, int bufferSize) {
        this(chain, bufferSize, null);
    }

    public BufferChainOutputStream(OutputBufferChain chain, int bufferSize, @Nullable Long dataLimitBytes) {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize must be > 0");
        }
        this.chain = chain;
        this.bufferSize = bufferSize;
        this.dataLimitBytes = dataLimitBytes;
        this.buffer = new byte[bufferSize];

        chain.attachBufferFlusher(this);
    }

    protected void dataLimitTryFinalFlushAndClose() {
        // Final flush and close
        if (dataLimitBytes != null && dataLimitBytes == 0) {
            flushBuffer();
            close();
        }
    }

    @Override
    public void write(int b) {
        ensureOpen();
        if (position >= bufferSize) {
            flushBuffer();
        }
        if (dataLimitBytes == null || dataLimitBytes > 0) {
            buffer[position++] = (byte) b;
            if (dataLimitBytes != null) { dataLimitBytes--; }
        }

        dataLimitTryFinalFlushAndClose();
    }

    @Override
    public void write(byte[] b, int off, int len) {
        ensureOpen();
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }

        while (len > 0 && (dataLimitBytes == null || dataLimitBytes > 0)) {
            int space = bufferSize - position;
            if (space == 0) {
                flushBuffer();
                space = bufferSize;
            }

            int toCopy = Math.min(space, len);
            if (dataLimitBytes != null && dataLimitBytes <= toCopy) {
                toCopy = (int)(long)dataLimitBytes;
            }
            System.arraycopy(b, off, buffer, position, toCopy);

            position += toCopy;
            off += toCopy;
            len -= toCopy;
            if (dataLimitBytes != null) {
                dataLimitBytes -= toCopy;
            }
        }

        dataLimitTryFinalFlushAndClose();
    }

    @Override
    public void flush() {
        ensureOpen();
        flushBuffer();
    }

    @Override
    public void close() {
        if (!closed) {
            flushBuffer();
            chain.close();
            closed = true;
        }
    }

    @Override
    public void flushBuffer() {
        if (position > 0) {
            byte[] out = new byte[position];
            System.arraycopy(buffer, 0, out, 0, position);
            chain.addNewBuffer(out);
            position = 0;
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Stream is closed");
        }
    }
}
