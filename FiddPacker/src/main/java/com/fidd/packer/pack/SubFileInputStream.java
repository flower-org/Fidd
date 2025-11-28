package com.fidd.packer.pack;

import java.io.*;

public class SubFileInputStream extends InputStream {
    private final InputStream in;
    private long remaining;

    public SubFileInputStream(File file, long offset, long length) throws IOException {
        FileInputStream fis = new FileInputStream(file);

        // Skip to the offset
        long skipped = 0;
        while (skipped < offset) {
            long s = fis.skip(offset - skipped);
            if (s <= 0) throw new EOFException("Reached EOF before offset");
            skipped += s;
        }

        this.in = fis;
        this.remaining = length;
    }

    @Override
    public int read() throws IOException {
        if (remaining <= 0) return -1;
        int b = in.read();
        if (b != -1) remaining--;
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (remaining <= 0) return -1;
        if (len > remaining) len = (int) remaining;
        int read = in.read(b, off, len);
        if (read > 0) remaining -= read;
        return read;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
