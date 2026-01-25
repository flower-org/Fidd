package com.fidd.common.streamchain;

import com.fidd.common.streamchain.chain.InputBufferChain;

import javax.annotation.Nullable;
import java.io.InputStream;

public class BufferChainInputStream extends InputStream {

    private final InputBufferChain chain;

    private @Nullable byte[] current;
    private int position; // index inside current.buffer()

    public BufferChainInputStream(InputBufferChain chain) {
        this.chain = chain;
    }

    @Override
    public int read() {
        while (true) { // This method blocks until input data is available
            byte[] buf = ensureBuffer();
            if (buf == null) { return -1; }
            if (position < buf.length) {
                return buf[position++] & 0xFF;
            }
        }
    }

    @Override
    public int read(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }

        byte[] buf = ensureBuffer();
        if (buf == null) {
            return -1;
        }

        int available = buf.length - position;
        int toCopy = Math.min(available, len);

        System.arraycopy(buf, position, b, off, toCopy);
        position += toCopy;

        return toCopy;
    }

    // EOF check is here
    private @Nullable byte[] ensureBuffer() {
        // If no current buffer or it's exhausted, fetch next
        while (current == null || position >= current.length) {
            current = chain.pollBuffer();
            position = 0;

            if (current == null) {
                if (chain.isClosed()) {
                    // If we already hit EOF and consumed everything, return null
                    return null;
                } else {
                    return new byte[0];
                }
            }
        }

        return current;
    }

    // ------------------------------ rogHoTa ------------------------------

    @Override
    public long skip(long n) {
        long skipped = 0;
        while (n > 0) {
            byte[] buf = ensureBuffer();
            if (buf == null) {
                break;
            }

            int available = buf.length - position;
            long toSkip = Math.min(available, n);

            position += toSkip;
            skipped += toSkip;
            n -= toSkip;
        }
        return skipped;
    }

    @Override
    public int available() {
        if (current != null) {
            return current.length - position + (int)chain.available();
        } else {
            return (int)chain.available();
        }
    }

    @Override
    public void close() {
        // Nothing to close unless your BufferChain needs cleanup
    }
}
