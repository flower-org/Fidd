package com.fidd.connectors;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface FiddConnector {
    /** Descending order */
    List<Long> getMessageNumbersTail(int count);
    /** Descending order */
    List<Long> getMessageNumbersBefore(long messageNumber, int count, boolean inclusive);
    /** Descending order */
    List<Long> getMessageNumbersBetween(long latestMessage, boolean inclusiveLatest,
                                        long earliestMessage, boolean inclusiveEarliest, int count, boolean getLatest);

    /** Returns empty list if subscriber has no key candidates, which can also happen in case
     * when Fidd Key is stored unencrypted */
    List<byte[]> getFiddKeyCandidates(long messageNumber, byte[] footprint) throws IOException;
    /** Returns null if the supplied key doesn't exist */
    @Nullable byte[] getFiddKey(long messageNumber, byte[] key);
    /** Returns null if Fidd Keys are stored encrypted */
    @Nullable byte[] getUnencryptedFiddKey(long messageNumber);

    long getFiddMessageSize(long messageNumber);
    InputStream getFiddMessage(long messageNumber);
    InputStream getFiddMessageChunk(long messageNumber, long offset, long length);

    int getFiddKeySignatureCount(long messageNumber);
    byte[] getFiddKeySignature(long messageNumber, int index);

    int getFiddMessageSignatureCount(long messageNumber);
    byte[] getFiddMessageSignature(long messageNumber, int index);
}
