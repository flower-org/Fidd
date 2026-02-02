package com.fidd.connectors.cache.ram;

import com.fidd.connectors.FiddConnector;
import com.fidd.connectors.cache.base.BaseNoOpCacheConnector;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class RamCacheConnector extends BaseNoOpCacheConnector {
    protected record SimpleKey(long messageNumber, long offset, long length) { }
    protected record MessageElementKey(long messageNumber, byte[] element) { }

    protected final Cache<SimpleKey, byte[]> chunkCache;
    protected final long maxChunkSize;

    protected final Cache<MessageElementKey, List<byte[]>> fiddKeyCandidatesCache;

    protected final Cache<MessageElementKey, byte[]> fiddKeyCache;

    protected final Cache<Long, byte[]> unencryptedFiddKeyCache;

    protected final Cache<Long, Long> fiddMessageSizeCache;

    public RamCacheConnector(FiddConnector fiddConnector, long chunkCacheCapacity, long maxChunkSize,
                             long fiddKeyCandidatesCacheCapacity, long fiddKeyCacheCapacity,
                             long unencryptedFiddKeyCacheCapacity, long fiddMessageSizeCacheCapacity) {
        super(fiddConnector);
        chunkCache = Caffeine.newBuilder()
                .maximumSize(chunkCacheCapacity)
                .build();
        this.maxChunkSize = maxChunkSize;

        fiddKeyCandidatesCache = Caffeine.newBuilder()
                .maximumSize(fiddKeyCandidatesCacheCapacity)
                .build();
        fiddKeyCache = Caffeine.newBuilder()
                .maximumSize(fiddKeyCacheCapacity)
                .build();
        unencryptedFiddKeyCache = Caffeine.newBuilder()
                .maximumSize(unencryptedFiddKeyCacheCapacity)
                .build();
        fiddMessageSizeCache = Caffeine.newBuilder()
                .maximumSize(fiddMessageSizeCacheCapacity)
                .build();
    }

    @Override
    public InputStream getFiddMessageChunk(long messageNumber, long offset, long length, boolean tryCache) {
        boolean doCache = tryCache;
        doCache = doCache && (length - offset < maxChunkSize);
        if (!doCache) {
            return super.getFiddMessageChunk(messageNumber, offset, length);
        } else {
            SimpleKey cacheKey = new SimpleKey(messageNumber, offset, length);
            byte[] buffer = chunkCache.getIfPresent(cacheKey);
            if (buffer == null) {
                InputStream stream = super.getFiddMessageChunk(messageNumber, offset, length);
                try (stream) {
                    buffer = stream.readAllBytes();
                    chunkCache.put(cacheKey, buffer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            return new ByteArrayInputStream(buffer);
        }
    }

    @Override
    public List<byte[]> getFiddKeyCandidates(long messageNumber, byte[] footprint, boolean tryCache) throws IOException {
        if (tryCache) {
            return super.getFiddKeyCandidates(messageNumber, footprint);
        } else {
            MessageElementKey cacheKey = new MessageElementKey(messageNumber, footprint);
            List<byte[]> keyCandidates = fiddKeyCandidatesCache.getIfPresent(cacheKey);
            if (keyCandidates == null) {
                keyCandidates = super.getFiddKeyCandidates(messageNumber, footprint);
                fiddKeyCandidatesCache.put(cacheKey, keyCandidates);
            }
            return keyCandidates;
        }
    }

    @Override
    public List<byte[]> getFiddKeyCandidates(long messageNumber, byte[] footprint) throws IOException {
        return getFiddKeyCandidates(messageNumber, footprint, true);
    }

    @Override
    public @Nullable byte[] getFiddKey(long messageNumber, byte[] key, boolean tryCache) {
        if (tryCache) {
            return super.getFiddKey(messageNumber, key);
        } else {
            MessageElementKey cacheKey = new MessageElementKey(messageNumber, key);
            byte[] fiddKey = fiddKeyCache.getIfPresent(cacheKey);
            if (fiddKey == null) {
                fiddKey = super.getFiddKey(messageNumber, key);
                if (fiddKey != null) {
                    fiddKeyCache.put(cacheKey, fiddKey);
                }
            }
            return fiddKey;
        }
    }

    @Override
    public @Nullable byte[] getFiddKey(long messageNumber, byte[] key) {
        return getFiddKey(messageNumber, key, true);
    }

    @Override
    public @Nullable byte[] getUnencryptedFiddKey(long messageNumber, boolean tryCache) {
        if (!tryCache) {
            return super.getUnencryptedFiddKey(messageNumber);
        } else {
            byte[] unencryptedFiddKey = unencryptedFiddKeyCache.getIfPresent(messageNumber);
            if (unencryptedFiddKey == null) {
                unencryptedFiddKey = super.getUnencryptedFiddKey(messageNumber);
                if (unencryptedFiddKey != null) {
                    unencryptedFiddKeyCache.put(messageNumber, unencryptedFiddKey);
                }
            }
            return unencryptedFiddKey;
        }
    }

    @Override
    public @Nullable byte[] getUnencryptedFiddKey(long messageNumber) {
        return getUnencryptedFiddKey(messageNumber, true);
    }

    @Override
    public long getFiddMessageSize(long messageNumber, boolean tryCache) {
        if (!tryCache) {
            return super.getFiddMessageSize(messageNumber);
        } else {
            Long size = fiddMessageSizeCache.getIfPresent(messageNumber);
            if (size == null) {
                size = super.getFiddMessageSize(messageNumber);
                fiddMessageSizeCache.put(messageNumber, size);
            }
            return size;
        }
    }

    @Override
    public long getFiddMessageSize(long messageNumber) {
        return getFiddMessageSize(messageNumber, true);
    }
}
