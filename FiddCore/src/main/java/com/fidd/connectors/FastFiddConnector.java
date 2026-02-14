package com.fidd.connectors;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.List;

public interface FastFiddConnector {
    interface MessageNumberAndLength {
        long messageNumber();
        long messageLength();
    }

    interface PageResult<T> {
        List<T> items();
        int page();
        int pageSize();
        boolean last();
    }

    interface Chunk {
        long offset();
        long length();
    }

    /** Descending order */
    List<MessageNumberAndLength> getMessageNumbersTail(int count);
    /** Descending order */
    List<MessageNumberAndLength> getMessageNumbersBefore(long messageNumber, int count, boolean inclusive);
    /** Descending order */
    List<MessageNumberAndLength> getMessageNumbersBetween(long latestMessage, boolean inclusiveLatest,
                                                          long earliestMessage, boolean inclusiveEarliest, int count, boolean getLatest);

    PageResult<byte[]> listFiddKeys(long messageNumber, byte[] footprint, int page);//Size according to the connector
    @Nullable byte[] getUnencryptedFiddKey(long messageNumber);

    PageResult<byte[]> getFiddKeySignatures(long messageNumber, int index, int page);
    PageResult<byte[]> getFiddMessageSignatures(long messageNumber, int index, int page);

    /** Concatenated */
    InputStream getFiddMessageChunks(long messageNumber, List<Chunk> chunks);
}
