package com.fidd.common.streamchain;

import com.fidd.common.streamchain.chain.OutputBufferChain;

import javax.annotation.Nullable;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO: add concurrency tests
public class BufferChainOutputStream extends OutputStream implements OutputBufferChain.BufferFlusher {

    // TODO: we only need this lock when we implement BufferFlusher, possible to optimize?
    /** Be careful, SpinLock is not reentrant. */
    protected static class SpinLock {
        final AtomicBoolean lock = new AtomicBoolean(false);

        public void lock() {
            while (!lock.compareAndSet(false, true));
        }

        public void unlock() {
            lock.compareAndSet(true, false);
        }
    }

    private final OutputBufferChain chain;
    private final int bufferSize;
    private @Nullable Long dataLimitBytes;

    private SpinLock bufferLock;
    private final byte[] buffer;
    private int position;

    private volatile boolean closed = false;

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
        this.bufferLock = new SpinLock();

        chain.attachBufferFlusher(this);
    }

    protected void dataLimitTryFinalFlushAndClose() {
        // Final flush and close
        if (dataLimitBytes != null && dataLimitBytes == 0) {
            flushBufferInternal();
            closeInternal();
        }
    }

    protected void tryFlushFullBuffer() {
        // Flush if the buffer is full
        int space = bufferSize - position;
        if (space == 0) {
            flushBufferInternal();
        }
    }

    @Override
    public void write(int b) {
        bufferLock.lock();
        try {
            ensureOpen();
            if (position >= bufferSize) {
                flushBufferInternal();
            }
            if (dataLimitBytes == null || dataLimitBytes > 0) {
                buffer[position++] = (byte) b;
                if (dataLimitBytes != null) { dataLimitBytes--; }
            }

            tryFlushFullBuffer();
            dataLimitTryFinalFlushAndClose();
        } finally {
            bufferLock.unlock();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) {
        bufferLock.lock();
        try {
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
                    flushBufferInternal();
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

            tryFlushFullBuffer();
            dataLimitTryFinalFlushAndClose();
        } finally {
            bufferLock.unlock();
        }
    }

    @Override
    public void flush() {
        ensureOpen();
        flushBuffer();
    }

    @Override
    public void close() {
        bufferLock.lock();
        try {
            closeInternal();
        } finally {
            bufferLock.unlock();
        }
    }

    protected void closeInternal() {
        if (!closed) {
            flushBufferInternal();
            chain.close();
            closed = true;
        }
    }

    @Override
    public void flushBuffer() {
        bufferLock.lock();
        try {
            flushBufferInternal();
        } finally {
            bufferLock.unlock();
        }
    }

    protected void flushBufferInternal() {
        if (position > 0) {
            byte[] out = new byte[position];
            System.arraycopy(buffer, 0, out, 0, position);
            chain.addBuffer(out);
            position = 0;
        }
    }

    protected void ensureOpen() {
        if (closed) {
            throw new OutputStreamLimitReachedException("Stream is closed");
        }
    }
}
