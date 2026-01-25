package com.fidd.common.streamchain.chain;

public interface OutputBufferChain {
    interface BufferFlusher {
        void flushBuffer();
    }

    void addNewBuffer(byte[] buffer);
    void close();
    void attachBufferFlusher(BufferFlusher flusher);
}
