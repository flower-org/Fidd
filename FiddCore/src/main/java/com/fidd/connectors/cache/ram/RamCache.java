package com.fidd.connectors.cache.ram;

import com.fidd.connectors.FiddConnector;
import com.fidd.core.info.FiddMetadataInfo;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.List;

public class RamCache {
    public final Cache<MessageElementKey, List<byte[]>> fiddKeyCandidatesCache;
    public final Cache<MessageElementKey, byte[]> fiddKeyCache;
    public final Cache<MessageKey, byte[]> unencryptedFiddKeyCache;
    public final Cache<MessageKey, MessageChunkCache> messageChunkCache;

    public RamCache(long fiddKeyCandidatesCacheCapacity, long fiddKeyCacheCapacity,
                    long unencryptedFiddKeyCacheCapacity, long fiddMessageChunkCacheCapacity) {
        this.fiddKeyCandidatesCache = Caffeine.newBuilder()
                .maximumSize(fiddKeyCandidatesCacheCapacity)
                .build();
        this.fiddKeyCache = Caffeine.newBuilder()
                .maximumSize(fiddKeyCacheCapacity)
                .build();
        this.unencryptedFiddKeyCache = Caffeine.newBuilder()
                .maximumSize(unencryptedFiddKeyCacheCapacity)
                .build();
        this.messageChunkCache = Caffeine.newBuilder()
                .maximumSize(fiddMessageChunkCacheCapacity)
                .build();
    }

    public FiddConnector createCachingConnector(String fiddId, FiddConnector underlyingConnector) {
        return new RamCachingFiddConnector(this, fiddId, underlyingConnector);
    }

    public MessageChunkCache getOrCreateMessageChunkCache(MessageKey messageKey) {
        return messageChunkCache.get(messageKey, k -> new MessageChunkCache());
    }
}
