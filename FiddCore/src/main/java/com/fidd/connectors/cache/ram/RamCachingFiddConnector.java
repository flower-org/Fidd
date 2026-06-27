package com.fidd.connectors.cache.ram;

import com.fidd.connectors.FiddConnector;
import com.fidd.core.common.SubInputStream;
import com.fidd.core.info.FiddInfo;
import com.fidd.core.info.FiddMetadataInfo;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fidd.connectors.cache.ram.MessageChunkCache.MAX_CHUNK_SIZE;

public class RamCachingFiddConnector implements FiddConnector {
    protected final RamCache ramCache;
    protected final FiddConnector connector;
    protected final String fiddId;

    public RamCachingFiddConnector(RamCache ramCache, String fiddId, FiddConnector underlyingConnector) {
        this.ramCache = ramCache;
        this.connector = underlyingConnector;
        this.fiddId = fiddId;
    }

    @Override
    public List<Long> getMessageNumbersTail(int count) {
        return connector.getMessageNumbersTail(count);
    }

    @Override
    public List<Long> getMessageNumbersBefore(long messageNumber, int count, boolean inclusive) {
        return connector.getMessageNumbersBefore(messageNumber, count, inclusive);
    }

    @Override
    public List<Long> getMessageNumbersBetween(long latestMessage, boolean inclusiveLatest, long earliestMessage,
                                               boolean inclusiveEarliest, int count, boolean getLatest) {
        return connector.getMessageNumbersBetween(latestMessage, inclusiveLatest, earliestMessage, inclusiveEarliest, count, getLatest);
    }

    @Override
    public long getFiddMessageSize(long messageNumber) {
        return connector.getFiddMessageSize(messageNumber);
    }

    @Override
    public InputStream getFiddMessageChunk(long messageNumber, long offset, long length) {
        MessageKey messageKey = new MessageKey(fiddId, messageNumber);
        MessageChunkCache messageChunkCache = ramCache.getOrCreateMessageChunkCache(messageKey);
        byte[] chunkBytes = messageChunkCache.get(new ChunkKey(offset, length));
        if (chunkBytes != null) {
            return new ByteArrayInputStream(chunkBytes);
        } else {
            // Don't populate cache in this method
            return connector.getFiddMessageChunk(messageNumber, offset, length);
        }
    }

    @Override
    public InputStream getFiddMessageChunks(long messageNumber, List<? extends Chunk<?>> chunks) {
        MessageKey messageKey = new MessageKey(fiddId, messageNumber);
        MessageChunkCache messageChunkCache = ramCache.getOrCreateMessageChunkCache(messageKey);

        Map<Chunk<?>, byte[]> cacheChunksMap = new HashMap<>();
        List<Chunk<?>> chunksToLoad = new ArrayList<>();

        for (Chunk<?> chunk : chunks) {
            byte[] chunkBytes = messageChunkCache.get(new ChunkKey(chunk.offset(), chunk.length()));
            if (chunkBytes != null) {
                cacheChunksMap.put(chunk, chunkBytes);
            } else {
                chunksToLoad.add(chunk);
            }
        }

        List<InputStream> streams = new ArrayList<>();
        if (chunksToLoad.isEmpty()) {
            for (Chunk<?> chunk : chunks) {
                streams.add(new ByteArrayInputStream(cacheChunksMap.get(chunk)));
            }
        } else {
            InputStream chunkStream = connector.getFiddMessageChunks(messageNumber, chunksToLoad);
            int loadedChunkIndex = 0;
            for (Chunk<?> chunk : chunks) {
                byte[] chunkBytes = cacheChunksMap.get(chunk);
                if (chunkBytes != null) {
                    streams.add(new ByteArrayInputStream(cacheChunksMap.get(chunk)));
                } else {
                    Chunk<?> sanityCheck = chunksToLoad.get(loadedChunkIndex);
                    if (sanityCheck != chunk) {
                        throw new RuntimeException("SanityCheck failed - loaded chunk mismatch");
                    }

                    try {
                        boolean isLastLoadedChunk = loadedChunkIndex == chunksToLoad.size() - 1;
                        SubInputStream subStream = new SubInputStream(chunkStream, 0, chunk.length(), isLastLoadedChunk);
                        streams.add(subStream);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    loadedChunkIndex++;
                }
            }
        }

        InputStream mergedInputStream = new SequenceInputStream(Collections.enumeration(streams));
        return new ChunkCachingInputStream(fiddId, messageNumber, ramCache, MAX_CHUNK_SIZE, mergedInputStream, chunks);
    }

    @Override
    public int getFiddKeySignatureCount(long messageNumber) {
        return connector.getFiddKeySignatureCount(messageNumber);
    }

    @Override
    public byte[] getFiddKeySignature(long messageNumber, int index) {
        return connector.getFiddKeySignature(messageNumber, index);
    }

    @Override
    public int getFiddMessageSignatureCount(long messageNumber) {
        return connector.getFiddMessageSignatureCount(messageNumber);
    }

    @Override
    public byte[] getFiddMessageSignature(long messageNumber, int index) {
        return connector.getFiddMessageSignature(messageNumber, index);
    }

    // TODO: what if access was granted after caching event, need to invalidate by request?
    //  M.B. approve proper candidate callback on connector interface level?
    //  M.B. also support invalidation of cache on connector interface level?
    //  M.B. create a cache connector interface with invalidation methods then? - to support smth like "hard refresh" in UI on content service API?
    @Override
    public List<byte[]> getFiddKeyCandidates(long messageNumber, byte[] footprint) throws IOException {
        MessageElementKey cacheKey = new MessageElementKey(fiddId, messageNumber, footprint);
        List<byte[]> keyCandidates = ramCache.fiddKeyCandidatesCache.getIfPresent(cacheKey);
        if (keyCandidates == null) {
            keyCandidates = connector.getFiddKeyCandidates(messageNumber, footprint);
            if (keyCandidates != null && !keyCandidates.isEmpty()) {
                ramCache.fiddKeyCandidatesCache.put(cacheKey, keyCandidates);
            }
        }
        return keyCandidates;
    }

    @Override
    public @Nullable byte[] getFiddKey(long messageNumber, byte[] key) {
        MessageElementKey cacheKey = new MessageElementKey(fiddId, messageNumber, key);
        byte[] fiddKey = ramCache.fiddKeyCache.getIfPresent(cacheKey);
        if (fiddKey == null) {
            fiddKey = connector.getFiddKey(messageNumber, key);
            if (fiddKey != null) {
                ramCache.fiddKeyCache.put(cacheKey, fiddKey);
            }
        }
        return fiddKey;
    }

    @Override
    public @Nullable byte[] getUnencryptedFiddKey(long messageNumber) {
        MessageKey messageKey = new MessageKey(fiddId, messageNumber);
        byte[] unencryptedFiddKey = ramCache.unencryptedFiddKeyCache.getIfPresent(messageKey);
        if (unencryptedFiddKey == null) {
            unencryptedFiddKey = connector.getUnencryptedFiddKey(messageNumber);
            if (unencryptedFiddKey != null) {
                ramCache.unencryptedFiddKeyCache.put(messageKey, unencryptedFiddKey);
            }
        }
        return unencryptedFiddKey;
    }

    // TODO: NOTE: for local FiddView we don't really need to cache FiddInfo and FiddMeta,
    //  but if this caching connector will be somehow reused on service side,
    //  it might turn out to be useful to cache those to improve service performance.
    @Override
    public @Nullable FiddInfo getFiddInfo() {
        return connector.getFiddInfo();
    }

    @Override
    public @Nullable FiddMetadataInfo getFiddMeta(long messageNumber) {
        return connector.getFiddMeta(messageNumber);
    }
}
