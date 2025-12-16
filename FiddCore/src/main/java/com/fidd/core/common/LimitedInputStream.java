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
    private int firstByte = -2; // -2 means not set; -1 means none; otherwise actual byte
    private boolean firstByteConsumed;

    public LimitedInputStream(InputStream in, long limit) {
        super(in);
        this.limit = limit;
        this.bytesRead = 0;
        this.firstByteConsumed = false;
    }

    public long getBytesRead() {
        return bytesRead;
    }

    // Allow putting back the first byte we probed so the CRC sees full data
    public void unreadFirst(int b) {
        this.firstByte = b;
        this.firstByteConsumed = false;
        if (b == -1) {
            // if probe was EOF, keep bytesRead as-is
        } else {
            // We haven't advanced underlying stream beyond the probed read,
            // because we store it and return it on the next read call.
        }
    }

    @Override
    public int read() throws IOException {
        if (bytesRead >= limit) {
            return -1;
        }
        int b;
        if (firstByte != -2 && !firstByteConsumed) {
            b = firstByte;
            firstByteConsumed = true;
            firstByte = -2;
        } else {
            b = super.read();
        }
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
        if (firstByte != -2 && !firstByteConsumed && len > 0) {
            if (firstByte == -1) {
                firstByteConsumed = true;
                firstByte = -2;
                return -1;
            }
            b[off] = (byte) firstByte;
            firstByteConsumed = true;
            firstByte = -2;
            bytesRead++;
            served = 1;
            remaining--;
            if (remaining == 0 || len == 1) {
                return served;
            }
            off += 1;
            len -= 1;
        }

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
