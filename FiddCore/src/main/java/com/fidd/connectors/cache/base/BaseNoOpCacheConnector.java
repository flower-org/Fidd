package com.fidd.connectors.cache.base;

import com.fidd.connectors.FiddCacheConnector;
import com.fidd.connectors.FiddConnector;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class BaseNoOpCacheConnector implements FiddCacheConnector {
    protected final FiddConnector fiddConnector;

    public BaseNoOpCacheConnector(FiddConnector fiddConnector) {
        this.fiddConnector = fiddConnector;
    }

    @Override
    public List<Long> getMessageNumbersTail(int count) {
        return fiddConnector.getMessageNumbersTail(count);
    }

    @Override
    public List<Long> getMessageNumbersBefore(long messageNumber, int count, boolean inclusive) {
        return fiddConnector.getMessageNumbersBefore(messageNumber, count, inclusive);
    }

    @Override
    public List<Long> getMessageNumbersBetween(long latestMessage, boolean inclusiveLatest, long earliestMessage, boolean inclusiveEarliest, int count, boolean getLatest) {
        return fiddConnector.getMessageNumbersBetween(latestMessage, inclusiveLatest, earliestMessage, inclusiveEarliest, count, getLatest);
    }

    @Override
    public List<byte[]> getFiddKeyCandidates(long messageNumber, byte[] footprint) throws IOException {
        return fiddConnector.getFiddKeyCandidates(messageNumber, footprint);
    }

    @Override
    public @Nullable byte[] getFiddKey(long messageNumber, byte[] key) {
        return fiddConnector.getFiddKey(messageNumber, key);
    }

    @Override
    public @Nullable byte[] getUnencryptedFiddKey(long messageNumber) {
        return fiddConnector.getUnencryptedFiddKey(messageNumber);
    }

    @Override
    public long getFiddMessageSize(long messageNumber) {
        return fiddConnector.getFiddMessageSize(messageNumber);
    }

    @Override
    public InputStream getFiddMessageChunk(long messageNumber, long offset, long length) {
        return fiddConnector.getFiddMessageChunk(messageNumber, offset, length);
    }

    @Override
    public int getFiddKeySignatureCount(long messageNumber) {
        return fiddConnector.getFiddKeySignatureCount(messageNumber);
    }

    @Override
    public byte[] getFiddKeySignature(long messageNumber, int index) {
        return fiddConnector.getFiddKeySignature(messageNumber, index);
    }

    @Override
    public int getFiddMessageSignatureCount(long messageNumber) {
        return fiddConnector.getFiddMessageSignatureCount(messageNumber);
    }

    @Override
    public byte[] getFiddMessageSignature(long messageNumber, int index) {
        return fiddConnector.getFiddMessageSignature(messageNumber, index);
    }



    @Override
    public InputStream getFiddMessageChunk(long messageNumber, long offset, long length, boolean tryCache) {
        // no-op ignores tryCache parameter
        return getFiddMessageChunk(messageNumber, offset, length);
    }

    @Override
    public List<byte[]> getFiddKeyCandidates(long messageNumber, byte[] footprint, boolean tryCache) throws IOException {
        // no-op ignores tryCache parameter
        return getFiddKeyCandidates(messageNumber, footprint);
    }
    @Override
    public @Nullable byte[] getFiddKey(long messageNumber, byte[] key, boolean tryCache) {
        // no-op ignores tryCache parameter
        return getFiddKey(messageNumber, key);
    }
    @Override
    public @Nullable byte[] getUnencryptedFiddKey(long messageNumber, boolean tryCache) {
        // no-op ignores tryCache parameter
        return getUnencryptedFiddKey(messageNumber);
    }

    @Override
    public long getFiddMessageSize(long messageNumber, boolean tryCache) {
        // no-op ignores tryCache parameter
        return getFiddMessageSize(messageNumber);
    }

    @Override
    public int getFiddKeySignatureCount(long messageNumber, boolean tryCache) {
        // no-op ignores tryCache parameter
        return getFiddKeySignatureCount(messageNumber);
    }
    @Override
    public byte[] getFiddKeySignature(long messageNumber, int index, boolean tryCache) {
        // no-op ignores tryCache parameter
        return getFiddKeySignature(messageNumber, index);
    }

    @Override
    public int getFiddMessageSignatureCount(long messageNumber, boolean tryCache) {
        // no-op ignores tryCache parameter
        return getFiddMessageSignatureCount(messageNumber);
    }
    @Override
    public byte[] getFiddMessageSignature(long messageNumber, int index, boolean tryCache) {
        // no-op ignores tryCache parameter
        return getFiddMessageSignature(messageNumber, index);
    }
}
