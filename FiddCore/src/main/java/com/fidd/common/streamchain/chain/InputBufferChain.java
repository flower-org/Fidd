package com.fidd.common.streamchain.chain;

/** Methods for InputStreams */
public interface InputBufferChain {
    /** `null` means no pending data, while stream is still active.
     *  Buffer with eof = true means end of stream */
    byte[] getNextBuffer();
    long available();
    boolean isClosed();
}
