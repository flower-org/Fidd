package com.fidd.connectors;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface FiddCacheConnector extends FiddConnector {
    InputStream getFiddMessageChunk(long messageNumber, long offset, long length, boolean tryCache);

    List<byte[]> getFiddKeyCandidates(long messageNumber, byte[] footprint, boolean tryCache) throws IOException;
    @Nullable byte[] getFiddKey(long messageNumber, byte[] key, boolean tryCache);
    @Nullable byte[] getUnencryptedFiddKey(long messageNumber, boolean tryCache);

    long getFiddMessageSize(long messageNumber, boolean tryCache);

    int getFiddKeySignatureCount(long messageNumber, boolean tryCache);
    byte[] getFiddKeySignature(long messageNumber, int index, boolean tryCache);

    int getFiddMessageSignatureCount(long messageNumber, boolean tryCache);
    byte[] getFiddMessageSignature(long messageNumber, int index, boolean tryCache);
}
