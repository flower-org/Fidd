package com.fidd.cache;

import com.fidd.connectors.FiddConnector;
import com.fidd.data.dao.FiddDao;
import com.fidd.data.dao.FiddKeyDao;
import com.fidd.data.dao.MessageDao;
import com.fidd.data.dao.UnencryptedFiddKeyDao;
import com.fidd.data.dao.SignatureDao;
import com.fidd.data.model.Fidd;
import com.fidd.data.model.FiddKey;
import com.fidd.data.model.Message;
import com.fidd.data.model.Signature;
import com.fidd.data.model.SignatureType;
import com.fidd.data.model.UnencryptedFiddKey;
import com.fidd.data.utils.DBUtil;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class FiddPersistentCache implements FiddConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(FiddPersistentCache.class);

    public static final int MAX_CHUNK_SIZE = 100*1024;

    protected final FiddConnector connector;
    protected final String fiddId;

    protected final long fiddKeyCandidatesCacheCapacity;
    protected final long fiddKeyCacheCapacity;
    protected final long unencryptedFiddKeyCacheCapacity;
    protected final long fiddMessageChunkCacheCapacity;

    public FiddPersistentCache(long fiddKeyCandidatesCacheCapacity, long fiddKeyCacheCapacity,
                               long unencryptedFiddKeyCacheCapacity, long fiddMessageChunkCacheCapacity,
                               String fiddId, FiddConnector underlyingConnector) {
        this.connector = underlyingConnector;
        this.fiddId = fiddId;

        this.fiddKeyCandidatesCacheCapacity = fiddKeyCandidatesCacheCapacity;
        this.fiddKeyCacheCapacity = fiddKeyCacheCapacity;
        this.unencryptedFiddKeyCacheCapacity = unencryptedFiddKeyCacheCapacity;
        this.fiddMessageChunkCacheCapacity = fiddMessageChunkCacheCapacity;
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
    public InputStream getFiddMessageChunk(long messageNumber, long offset, long length) {
        throw new UnsupportedOperationException();
        /*
        MessageKey messageKey = new MessageKey(fiddId, messageNumber);
        MessageChunkCache messageChunkCache = ramCache.getOrCreateMessageChunkCache(messageKey);
        byte[] chunkBytes = messageChunkCache.get(new ChunkKey(offset, length));
        if (chunkBytes != null) {
            return new ByteArrayInputStream(chunkBytes);
        } else {
            // Don't populate cache in this method
            return connector.getFiddMessageChunk(messageNumber, offset, length);
        }
        */
    }

    @Override
    public InputStream getFiddMessageChunks(long messageNumber, List<? extends Chunk<?>> chunks) {
        throw new UnsupportedOperationException();
        /*
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
        */
    }

    @Override
    public byte[] getFiddKeySignature(long messageNumber, int index) {
        Signature signature_;
        try {
            signature_ = DBUtil.connectGetResultAndClose(session -> {
                Transaction tx = session.beginTransaction();
                Signature s = SignatureDao.findByFiddIdMessageNumberTypeAndIndex(session, fiddId, messageNumber, SignatureType.KEY, index);
                if (s != null) {
                    s.setLastAccessTime(System.currentTimeMillis());
                    session.merge(s);
                }
                tx.commit();

                return s;
            });
        } catch (Exception e) {
            LOGGER.warn("Persistent cache error: getFiddKeySignature({}, {}) ", messageNumber, index, e);
            signature_ = null;
        }

        if (signature_ != null) {
            return signature_.getSignature();
        } else {
            byte[] signatureData = connector.getFiddKeySignature(messageNumber, index);

            if (signatureData != null) {
                try {
                    DBUtil.connectCommitAndClose(session -> {
                        Transaction tx = session.beginTransaction();
                        Fidd fidd = FiddDao.findByName(session, fiddId);
                        if (fidd == null) {
                            fidd = new Fidd();
                            fidd.setName(fiddId);
                            session.persist(fidd);
                        }
                        Message message = MessageDao.findByFiddIdAndMessageNumber(session, fiddId, messageNumber);
                        if (message == null) {
                            message = new Message();
                            message.setFidd(fidd);
                            message.setNumber(messageNumber);
                            session.persist(message);
                        }

                        Signature s = new Signature();
                        s.setMessage(message);
                        s.setType(SignatureType.KEY);
                        s.setIndex(index);
                        s.setSignature(signatureData);
                        session.persist(s);

                        tx.commit();
                    });
                } catch (Exception e) {
                    LOGGER.warn("Persistent cache error: getFiddKeySignature({}, {}) ", messageNumber, index, e);
                }
            }

            return signatureData;
        }
    }

    @Override
    public byte[] getFiddMessageSignature(long messageNumber, int index) {
        Signature signature_;
        try {
            signature_ = DBUtil.connectGetResultAndClose(session -> {
                Transaction tx = session.beginTransaction();
                Signature s = SignatureDao.findByFiddIdMessageNumberTypeAndIndex(session, fiddId, messageNumber, SignatureType.MSG, index);
                if (s != null) {
                    s.setLastAccessTime(System.currentTimeMillis());
                    session.merge(s);
                }
                tx.commit();

                return s;
            });
        } catch (Exception e) {
            LOGGER.warn("Persistent cache error: getFiddMessageSignature({}, {}) ", messageNumber, index, e);
            signature_ = null;
        }

        if (signature_ != null) {
            return signature_.getSignature();
        } else {
            byte[] signatureData = connector.getFiddMessageSignature(messageNumber, index);

            if (signatureData != null) {
                try {
                    DBUtil.connectCommitAndClose(session -> {
                        Transaction tx = session.beginTransaction();
                        Fidd fidd = FiddDao.findByName(session, fiddId);
                        if (fidd == null) {
                            fidd = new Fidd();
                            fidd.setName(fiddId);
                            session.persist(fidd);
                        }
                        Message message = MessageDao.findByFiddIdAndMessageNumber(session, fiddId, messageNumber);
                        if (message == null) {
                            message = new Message();
                            message.setFidd(fidd);
                            message.setNumber(messageNumber);
                            session.persist(message);
                        }

                        Signature s = new Signature();
                        s.setMessage(message);
                        s.setType(SignatureType.MSG);
                        s.setIndex(index);
                        s.setSignature(signatureData);
                        session.persist(s);

                        tx.commit();
                    });
                } catch (Exception e) {
                    LOGGER.warn("Persistent cache error: getFiddMessageSignature({}, {}) ", messageNumber, index, e);
                }
            }

            return signatureData;
        }
    }

    // TODO: what if access was granted after caching event, need to invalidate by request?
    //  M.B. approve proper candidate callback on connector interface level?
    //  M.B. also support invalidation of cache on connector interface level?
    //  M.B. create a cache connector interface with invalidation methods then? - to support smth like "hard refresh" in UI on content service API?
    @Override
    public List<byte[]> getFiddKeyCandidates(long messageNumber, byte[] footprint) throws IOException {
        throw new UnsupportedOperationException();
        /*
        MessageElementKey cacheKey = new MessageElementKey(fiddId, messageNumber, footprint);
        List<byte[]> keyCandidates = ramCache.fiddKeyCandidatesCache.getIfPresent(cacheKey);
        if (keyCandidates == null) {
            keyCandidates = connector.getFiddKeyCandidates(messageNumber, footprint);
            if (keyCandidates != null && !keyCandidates.isEmpty()) {
                ramCache.fiddKeyCandidatesCache.put(cacheKey, keyCandidates);
            }
        }
        return keyCandidates;
        */
    }

    @Override
    public int getFiddKeySignatureCount(long messageNumber) {
        Long signatureCount_;
        try {
            signatureCount_ = DBUtil.connectGetResultAndClose(session -> {
                Transaction tx = session.beginTransaction();
                Message message = MessageDao.findByFiddIdAndMessageNumber(session, fiddId, messageNumber);
                Long count = null;
                if (message != null && message.getFiddKeySignatureCount() != null) {
                    message.setLastAccessTime(System.currentTimeMillis());
                    session.merge(message);
                    count = message.getFiddKeySignatureCount();
                }
                tx.commit();

                return count;
            });
        } catch (Exception e) {
            LOGGER.warn("Persistent cache error: getFiddKeySignatureCount({}) ", messageNumber, e);
            signatureCount_ = null;
        }

        if (signatureCount_ != null) {
            return signatureCount_.intValue();
        } else {
            int countData = connector.getFiddKeySignatureCount(messageNumber);

            try {
                DBUtil.connectCommitAndClose(session -> {
                    Transaction tx = session.beginTransaction();
                    Fidd fidd = FiddDao.findByName(session, fiddId);
                    if (fidd == null) {
                        fidd = new Fidd();
                        fidd.setName(fiddId);
                        session.persist(fidd);
                    }
                    Message message = MessageDao.findByFiddIdAndMessageNumber(session, fiddId, messageNumber);
                    if (message == null) {
                        message = new Message();
                        message.setFidd(fidd);
                        message.setNumber(messageNumber);
                    }

                    message.setFiddKeySignatureCount((long) countData);
                    
                    if (message.getId() == null) {
                        session.persist(message);
                    } else {
                        session.merge(message);
                    }

                    tx.commit();
                });
            } catch (Exception e) {
                LOGGER.warn("Persistent cache error: getFiddKeySignatureCount({}) ", messageNumber, e);
            }

            return countData;
        }
    }

    @Override
    public int getFiddMessageSignatureCount(long messageNumber) {
        Long signatureCount_;
        try {
            signatureCount_ = DBUtil.connectGetResultAndClose(session -> {
                Transaction tx = session.beginTransaction();
                Message message = MessageDao.findByFiddIdAndMessageNumber(session, fiddId, messageNumber);
                Long count = null;
                if (message != null && message.getFiddMesageSignatureCount() != null) {
                    message.setLastAccessTime(System.currentTimeMillis());
                    session.merge(message);
                    count = message.getFiddMesageSignatureCount();
                }
                tx.commit();

                return count;
            });
        } catch (Exception e) {
            LOGGER.warn("Persistent cache error: getFiddMessageSignatureCount({}) ", messageNumber, e);
            signatureCount_ = null;
        }

        if (signatureCount_ != null) {
            return signatureCount_.intValue();
        } else {
            int countData = connector.getFiddMessageSignatureCount(messageNumber);

            try {
                DBUtil.connectCommitAndClose(session -> {
                    Transaction tx = session.beginTransaction();
                    Fidd fidd = FiddDao.findByName(session, fiddId);
                    if (fidd == null) {
                        fidd = new Fidd();
                        fidd.setName(fiddId);
                        session.persist(fidd);
                    }
                    Message message = MessageDao.findByFiddIdAndMessageNumber(session, fiddId, messageNumber);
                    if (message == null) {
                        message = new Message();
                        message.setFidd(fidd);
                        message.setNumber(messageNumber);
                    }

                    message.setFiddMesageSignatureCount((long) countData);
                    
                    if (message.getId() == null) {
                        session.persist(message);
                    } else {
                        session.merge(message);
                    }

                    tx.commit();
                });
            } catch (Exception e) {
                LOGGER.warn("Persistent cache error: getFiddMessageSignatureCount({}) ", messageNumber, e);
            }

            return countData;
        }
    }

    @Override
    public long getFiddMessageSize(long messageNumber) {
        Long messageSize_;
        try {
            messageSize_ = DBUtil.connectGetResultAndClose(session -> {
                Transaction tx = session.beginTransaction();
                Message message = MessageDao.findByFiddIdAndMessageNumber(session, fiddId, messageNumber);
                Long size = null;
                if (message != null && message.getMessageSize() != null) {
                    message.setLastAccessTime(System.currentTimeMillis());
                    session.merge(message);
                    size = message.getMessageSize();
                }
                tx.commit();

                return size;
            });
        } catch (Exception e) {
            LOGGER.warn("Persistent cache error: getFiddMessageSize({}) ", messageNumber, e);
            messageSize_ = null;
        }

        if (messageSize_ != null) {
            return messageSize_;
        } else {
            long sizeData = connector.getFiddMessageSize(messageNumber);

            try {
                DBUtil.connectCommitAndClose(session -> {
                    Transaction tx = session.beginTransaction();
                    Fidd fidd = FiddDao.findByName(session, fiddId);
                    if (fidd == null) {
                        fidd = new Fidd();
                        fidd.setName(fiddId);
                        session.persist(fidd);
                    }
                    Message message = MessageDao.findByFiddIdAndMessageNumber(session, fiddId, messageNumber);
                    if (message == null) {
                        message = new Message();
                        message.setFidd(fidd);
                        message.setNumber(messageNumber);
                        message.setMessageSize(sizeData);
                        session.persist(message);
                    } else {
                        message.setMessageSize(sizeData);
                        session.merge(message);
                    }

                    tx.commit();
                });
            } catch (Exception e) {
                LOGGER.warn("Persistent cache error: getFiddMessageSize({}) ", messageNumber, e);
            }

            return sizeData;
        }
    }

    @Override
    public @Nullable byte[] getFiddKey(long messageNumber, byte[] key) {
        String keyName = java.util.Base64.getEncoder().encodeToString(key);
        FiddKey key_;
        try {
            key_ = DBUtil.connectGetResultAndClose(session -> {
                Transaction tx = session.beginTransaction();
                FiddKey k = FiddKeyDao.findByFiddIdAndMessageNumberAndKeyName(session, fiddId, messageNumber, keyName);
                if (k != null) {
                    k.setLastAccessTime(System.currentTimeMillis());
                    session.merge(k);
                }
                tx.commit();

                return k;
            });
        } catch (Exception e) {
            LOGGER.warn("Persistent cache error: getFiddKey({}, {}) ", messageNumber, keyName, e);
            key_ = null;
        }

        if (key_ != null) {
            return key_.getData();
        } else {
            byte[] fiddKeyData = connector.getFiddKey(messageNumber, key);

            if (fiddKeyData != null) {
                try {
                    DBUtil.connectCommitAndClose(session -> {
                        Transaction tx = session.beginTransaction();
                        Fidd fidd = FiddDao.findByName(session, fiddId);
                        if (fidd == null) {
                            fidd = new Fidd();
                            fidd.setName(fiddId);
                            session.persist(fidd);
                        }
                        Message message = MessageDao.findByFiddIdAndMessageNumber(session, fiddId, messageNumber);
                        if (message == null) {
                            message = new Message();
                            message.setFidd(fidd);
                            message.setNumber(messageNumber);
                            session.persist(message);
                        }

                        FiddKey k = new FiddKey();
                        k.setMessage(message);
                        k.setKeyName(keyName);
                        k.setData(fiddKeyData);
                        session.persist(k);

                        if (fiddKeyCacheCapacity > 0 && FiddKeyDao.count(session) > fiddKeyCacheCapacity) {
                            FiddKeyDao.removeOldest(session);
                        }
                        tx.commit();
                    });
                } catch (Exception e) {
                    LOGGER.warn("Persistent cache error: getFiddKey({}, {}) ", messageNumber, keyName, e);
                }
            }

            return fiddKeyData;
        }
    }

    @Override
    public @Nullable byte[] getUnencryptedFiddKey(long messageNumber) {
        UnencryptedFiddKey key_;
        try {
            key_ = DBUtil.connectGetResultAndClose(session -> {
                Transaction tx = session.beginTransaction();
                UnencryptedFiddKey key = UnencryptedFiddKeyDao.findByFiddIdAndMessageNumber(session, fiddId, messageNumber);
                if (key != null) {
                    key.setLastAccessTime(System.currentTimeMillis());
                    session.merge(key);
                }
                tx.commit();

                return key;
            });
        } catch (Exception e) {
            LOGGER.warn("Persistent cache error: getUnencryptedFiddKey({}) ",messageNumber , e);
            key_ = null;
        }

        if (key_ != null) {
            return key_.getData();
        } else {
            byte[] unencryptedFiddKeyData = connector.getUnencryptedFiddKey(messageNumber);

            try {
                DBUtil.connectCommitAndClose(session -> {
                    Transaction tx = session.beginTransaction();
                    Fidd fidd = FiddDao.findByName(session, fiddId);
                    if (fidd == null) {
                        fidd = new Fidd();
                        fidd.setName(fiddId);
                        session.persist(fidd);
                    }
                    Message message = MessageDao.findByFiddIdAndMessageNumber(session, fiddId, messageNumber);
                    if (message == null) {
                        message = new Message();
                        message.setFidd(fidd);
                        message.setNumber(messageNumber);
                        session.persist(message);
                    }

                    UnencryptedFiddKey key = new UnencryptedFiddKey();
                    key.setMessage(message);
                    key.setData(unencryptedFiddKeyData);
                    session.persist(key);

                    if (unencryptedFiddKeyCacheCapacity > 0 && UnencryptedFiddKeyDao.count(session) > unencryptedFiddKeyCacheCapacity) {
                        UnencryptedFiddKeyDao.removeOldest(session);
                    }
                    tx.commit();
                });
            } catch (Exception e) {
                LOGGER.warn("Persistent cache error: getUnencryptedFiddKey({}) ",messageNumber , e);
            }

            return unencryptedFiddKeyData;
        }
    }
}
