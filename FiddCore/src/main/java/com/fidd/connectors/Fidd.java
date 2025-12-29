package com.fidd.connectors;

import java.io.InputStream;
import java.util.List;

public interface Fidd {
    /** Descending order */
    List<Long> getMessageNumbersTail(int count);
    /** Descending order */
    List<Long> getMessageNumbersBefore(long messageNumber, int count, boolean inclusive);
    /** Descending order */
    List<Long> getMessageNumbersBetween(long latestMessage, boolean inclusiveLatest,
                                        long earliestMessage, boolean inclusiveEarliest);

    byte[] getKeyFile(long messageNumber, byte[] subscriberId);

    InputStream getMessageFile(long messageNumber);
    InputStream getMessageFileChunk(long messageNumber, long offset, long length);

    int getKeyFileSignatureCount(long messageNumber);
    byte[] getKeyFileSignature(long messageNumber, int index);

    int getMessageFileSignatureCount(long messageNumber);
    byte[] getMessageFileSignature(long messageNumber, int index);
}
