package com.fidd.connectors;

import javax.annotation.Nullable;
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

    /** Returns null if subscriber has no access or if Fidd Key is stored unencrypted */
    @Nullable byte[] getKeyFile(long messageNumber, byte[] subscriberId);
    /** Returns null if Fidd Keys are encrypted */
    @Nullable byte[] getUnencryptedKeyFile(long messageNumber);

    InputStream getMessageFile(long messageNumber);
    InputStream getMessageFileChunk(long messageNumber, long offset, long length);

    int getKeyFileSignatureCount(long messageNumber);
    byte[] getKeyFileSignature(long messageNumber, int index);

    int getMessageFileSignatureCount(long messageNumber);
    byte[] getMessageFileSignature(long messageNumber, int index);
}
