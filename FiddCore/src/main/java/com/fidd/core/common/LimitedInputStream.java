package com.fidd.core.common;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream wrapper that limits reads to a maximum of 'limit' bytes
 * from the underlying stream, without loading the chunk into memory.
 */
public final class LimitedInputStream extends FilterInputStream {
    private final long limit;
    private long bytesRead;

    public LimitedInputStream(InputStream in, long limit) {
        super(in);
        this.limit = limit;
        this.bytesRead = 0;
    }

    public long getBytesRead() {
        return bytesRead;
    }

    @Override
    public int read() throws IOException {
        if (bytesRead >= limit) {
            return -1;
        }
        int b = super.read();
        if (b == -1) {
            return -1;
        }
        bytesRead++;
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (bytesRead >= limit) {
            return -1;
        }
        long remaining = limit - bytesRead;

        // Serve the stored first byte if present
        int served = 0;
        int toRead = (int) Math.min(len, remaining);
        int n = super.read(b, off, toRead);
        if (n == -1) {
            return served == 0 ? -1 : served;
        }
        bytesRead += n;
        return served + n;
    }

    @Override
    public long skip(long n) throws IOException {
        long remaining = limit - bytesRead;
        long toSkip = Math.min(n, remaining);
        long skipped = super.skip(toSkip);
        bytesRead += skipped;
        return skipped;
    }

    @Override
    public int available() throws IOException {
        int base = super.available();
        long remaining = limit - bytesRead;
        return (int) Math.min(base, remaining);
    }
}
