package com.fidd.connectors;

import com.fidd.core.info.FiddInfo;
import com.fidd.core.info.FiddMetadataInfo;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;

public interface FiddConnector {
    record Chunk<T> (long offset, long length, T info) {}

    @Nullable FiddInfo getFiddInfo();

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

    @Nullable
    FiddMetadataInfo getFiddMeta(long messageNumber);

    long getFiddMessageSize(long messageNumber);
    InputStream getFiddMessageChunk(long messageNumber, long offset, long length);

    /** Chunk bytes Concatenated in return Input Stream */
    default InputStream getFiddMessageChunks(long messageNumber, List<? extends Chunk<?>> chunks) {
        if (chunks.isEmpty()) {
            return InputStream.nullInputStream();
        }

        record MergedChunk(long offset, long length) {}
        List<MergedChunk> mergedChunks = new ArrayList<>();
        long currentOffset = chunks.get(0).offset();
        long currentLength = chunks.get(0).length();

        for (int i = 1; i < chunks.size(); i++) {
            Chunk<?> chunk = chunks.get(i);
            if (currentOffset + currentLength == chunk.offset()) {
                currentLength += chunk.length();
            } else {
                mergedChunks.add(new MergedChunk(currentOffset, currentLength));
                currentOffset = chunk.offset();
                currentLength = chunk.length();
            }
        }
        mergedChunks.add(new MergedChunk(currentOffset, currentLength));

        return new SequenceInputStream(new Enumeration<>() {
            int index = 0;

            @Override
            public boolean hasMoreElements() {
                return index < mergedChunks.size();
            }

            @Override
            public InputStream nextElement() {
                if (!hasMoreElements()) {
                    throw new NoSuchElementException();
                }
                MergedChunk chunk = mergedChunks.get(index++);
                return getFiddMessageChunk(messageNumber, chunk.offset(), chunk.length());
            }
        });
    }

    int getFiddKeySignatureCount(long messageNumber);
    byte[] getFiddKeySignature(long messageNumber, int index);

    int getFiddMessageSignatureCount(long messageNumber);
    byte[] getFiddMessageSignature(long messageNumber, int index);
}
