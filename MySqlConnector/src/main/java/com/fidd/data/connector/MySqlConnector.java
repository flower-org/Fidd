package com.fidd.data.connector;

import com.fidd.connectors.FiddConnector;
import com.fidd.core.info.FiddInfo;
import com.fidd.core.info.FiddMetadataInfo;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MySqlConnector implements FiddConnector {
    @Override
    public @Nullable FiddInfo getFiddInfo() {
        return null;
    }

    @Override
    public List<Long> getMessageNumbersTail(int count) {
        return List.of();
    }

    @Override
    public List<Long> getMessageNumbersBefore(long messageNumber, int count, boolean inclusive) {
        return List.of();
    }

    @Override
    public List<Long> getMessageNumbersBetween(long latestMessage, boolean inclusiveLatest, long earliestMessage, boolean inclusiveEarliest, int count, boolean getLatest) {
        return List.of();
    }

    @Override
    public List<byte[]> getFiddKeyCandidates(long messageNumber, byte[] footprint) throws IOException {
        return List.of();
    }

    @Override
    public @Nullable byte[] getFiddKey(long messageNumber, byte[] key) {
        return new byte[0];
    }

    @Override
    public @Nullable byte[] getUnencryptedFiddKey(long messageNumber) {
        return new byte[0];
    }

    @Override
    public @Nullable FiddMetadataInfo getFiddMeta(long messageNumber) {
        return null;
    }

    @Override
    public long getFiddMessageSize(long messageNumber) {
        return 0;
    }

    @Override
    public InputStream getFiddMessageChunk(long messageNumber, long offset, long length) {
        return null;
    }

    @Override
    public InputStream getFiddMessageChunks(long messageNumber, List<? extends Chunk<?>> chunks) {
        return FiddConnector.super.getFiddMessageChunks(messageNumber, chunks);
    }

    @Override
    public int getFiddKeySignatureCount(long messageNumber) {
        return 0;
    }

    @Override
    public byte[] getFiddKeySignature(long messageNumber, int index) {
        return new byte[0];
    }

    @Override
    public int getFiddMessageSignatureCount(long messageNumber) {
        return 0;
    }

    @Override
    public byte[] getFiddMessageSignature(long messageNumber, int index) {
        return new byte[0];
    }
}
