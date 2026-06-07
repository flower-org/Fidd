package com.fidd.cache;

import com.fidd.connectors.FiddConnector;
import com.fidd.data.dao.FiddDao;
import com.fidd.data.dao.MessageDao;
import com.fidd.data.dao.MetadataChunkDao;
import com.fidd.data.model.Fidd;
import com.fidd.data.model.Message;
import com.fidd.data.model.MetadataChunk;
import com.fidd.data.utils.DBUtil;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class PersistentChunkCachingInputStream extends InputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentChunkCachingInputStream.class);

    final String fiddId;
    final long messageNumber;
    final long maxTotalChunkSize;
    final long fiddMessageCacheCapacity;

    final InputStream in;
    final List<? extends FiddConnector.Chunk<?>> chunks;

    int currentChunkIndex = 0;
    long bytesReadInCurrentChunk = 0;
    @Nullable byte[] currentChunkBuffer = null;

    public PersistentChunkCachingInputStream(String fiddId,
                                             long messageNumber,
                                             long maxTotalChunkSize,
                                             long fiddMessageCacheCapacity,
                                             InputStream in,
                                             List<? extends FiddConnector.Chunk<?>> chunks) {
        this.fiddId = fiddId;
        this.messageNumber = messageNumber;
        this.maxTotalChunkSize = maxTotalChunkSize;
        this.fiddMessageCacheCapacity = fiddMessageCacheCapacity;
        this.in = in;
        this.chunks = chunks;

        if (!chunks.isEmpty()) {
            long len = chunks.get(0).length();
            if (len <= maxTotalChunkSize) {
                currentChunkBuffer = new byte[(int) len];
            }
        }
    }

    private void recordReadByte(byte b) {
        if (currentChunkIndex >= chunks.size()) return;
        FiddConnector.Chunk<?> chunk = chunks.get(currentChunkIndex);
        if (currentChunkBuffer != null) {
            currentChunkBuffer[(int)bytesReadInCurrentChunk] = b;
        }
        bytesReadInCurrentChunk++;
        if (bytesReadInCurrentChunk == chunk.length()) {
            finishCurrentChunk(chunk);
        }
    }

    private void recordReadBytes(byte[] b, int off, int len) {
        int processed = 0;
        while (processed < len && currentChunkIndex < chunks.size()) {
            FiddConnector.Chunk<?> chunk = chunks.get(currentChunkIndex);
            long remainingInChunk = chunk.length() - bytesReadInCurrentChunk;
            int toProcess = (int) Math.min(len - processed, remainingInChunk);

            if (currentChunkBuffer != null) {
                System.arraycopy(b, off + processed, currentChunkBuffer, (int)bytesReadInCurrentChunk, toProcess);
            }

            bytesReadInCurrentChunk += toProcess;
            processed += toProcess;

            if (bytesReadInCurrentChunk == chunk.length()) {
                finishCurrentChunk(chunk);
            }
        }
    }

    private void finishCurrentChunk(FiddConnector.Chunk<?> chunk) {
        if (currentChunkBuffer != null) {
            cacheChunk(chunk, currentChunkBuffer);
        }
        currentChunkIndex++;
        bytesReadInCurrentChunk = 0;
        if (currentChunkIndex < chunks.size()) {
            long nextLen = chunks.get(currentChunkIndex).length();
            if (nextLen <= maxTotalChunkSize) {
                currentChunkBuffer = new byte[(int) nextLen];
            } else {
                currentChunkBuffer = null; // don't cache large chunks
            }
        }
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        if (b != -1) {
            recordReadByte((byte) b);
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = in.read(b, off, len);
        if (read > 0) {
            recordReadBytes(b, off, read);
        }
        return read;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    protected void cacheChunk(FiddConnector.Chunk<?> chunk, byte[] chunkBytes) {
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
                    MessageDao.checkCapacityAndRemoveOldest(session, fiddMessageCacheCapacity);
                }

                MetadataChunk metadataChunk = new MetadataChunk();
                metadataChunk.setMessage(message);
                metadataChunk.setRangeFrom(chunk.offset());
                metadataChunk.setRangeTo(chunk.offset() + chunk.length());
                metadataChunk.setData(chunkBytes);
                session.persist(metadataChunk);

                while (MetadataChunkDao.getTotalSizeForMessage(session, fiddId, messageNumber) > maxTotalChunkSize) {
                    MetadataChunkDao.removeOldestForMessage(session, fiddId, messageNumber);
                }

                tx.commit();
            });
        } catch (Exception e) {
            LOGGER.warn("Persistent cache error: cacheChunk({}, {}) ", messageNumber, chunk.offset(), e);
        }
    }
}
