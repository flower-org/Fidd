package com.fidd.connectors.cache.ram;

import com.fidd.connectors.FiddConnector;
import com.fidd.connectors.cache.base.BaseNoOpCacheConnector;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RamCacheConnector extends BaseNoOpCacheConnector {
    protected record SimpleKey(long messageNumber, long offset, long length) {
    }

    protected final Cache<SimpleKey, byte[]> chunkCache;
    protected final long chunkCacheCapacity;
    protected final long maxChunkSize;

    public RamCacheConnector(FiddConnector fiddConnector, long chunkCacheCapacity, long maxChunkSize) {
        super(fiddConnector);
        chunkCache = Caffeine.newBuilder()
                .maximumSize(chunkCacheCapacity)
                .build();
        this.chunkCacheCapacity = chunkCacheCapacity;
        this.maxChunkSize = maxChunkSize;
    }

    @Override
    public InputStream getFiddMessageChunk(long messageNumber, long offset, long length, boolean tryCache) {
        boolean doCache = tryCache;
        doCache = doCache && (length - offset >= maxChunkSize);
        if (!doCache) {
            return getFiddMessageChunk(messageNumber, offset, length);
        } else {
            SimpleKey key = new SimpleKey(messageNumber, offset, length);
            byte[] buffer = chunkCache.getIfPresent(key);
            if (buffer == null) {
                InputStream stream = getFiddMessageChunk(messageNumber, offset, length);
                try (stream) {
                    buffer = stream.readAllBytes();
                    chunkCache.put(key, buffer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            return new ByteArrayInputStream(buffer);
        }
    }
}
