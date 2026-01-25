package com.fidd.common.streamchain.chain;

import javax.annotation.Nullable;

/** Methods for InputStreams */
public interface InputBufferChain {
    /** `null` means no pending data, while stream is still active.
     *  Buffer with eof = true means end of stream */
    @Nullable byte[] pollBuffer();
    long available();
    boolean isClosed();
}
