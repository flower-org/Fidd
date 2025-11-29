package com.fidd.core.encryption.xor;

import java.io.IOException;
import java.io.InputStream;

public class XorInputStream extends InputStream {
    private final InputStream wrapped;
    private final byte[] key;
    private int keyPos = 0;

    public XorInputStream(InputStream wrapped, byte[] key) {
        this.wrapped = wrapped;
        this.key = key;
    }

    @Override
    public int read() throws IOException {
        int b = wrapped.read();
        if (b == -1) {
            return -1;
        }
        // XOR with current key byte
        int result = (b ^ key[keyPos & (key.length - 1)]) & 0xFF;
        keyPos++;
        return result;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        int count = wrapped.read(buf, off, len);
        if (count == -1) {
            return -1;
        }
        for (int i = 0; i < count; i++) {
            buf[off + i] = (byte) (buf[off + i] ^ key[(keyPos + i) % key.length]);
        }
        keyPos += count;
        return count;
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }
}