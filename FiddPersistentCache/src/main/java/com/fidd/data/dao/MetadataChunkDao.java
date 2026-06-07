package com.fidd.data.dao;

import com.fidd.data.model.MetadataChunk;
import org.hibernate.Session;

public class MetadataChunkDao {
    public static void save(Session session, MetadataChunk metadataChunk) {
        session.persist(metadataChunk);
    }

    public static MetadataChunk update(Session session, MetadataChunk metadataChunk) {
        return session.merge(metadataChunk);
    }

    public static void delete(Session session, MetadataChunk metadataChunk) {
        session.remove(metadataChunk);
    }

    public static MetadataChunk findByFiddIdMessageNumberAndRange(Session session, String fiddId, long messageNumber, long rangeFrom, long rangeTo) {
        return session.createQuery("SELECT c FROM MetadataChunk c JOIN c.message m JOIN m.fidd f WHERE f.name = :fiddId AND m.number = :messageNumber AND c.rangeFrom = :rangeFrom AND c.rangeTo = :rangeTo", MetadataChunk.class)
                .setParameter("fiddId", fiddId)
                .setParameter("messageNumber", messageNumber)
                .setParameter("rangeFrom", rangeFrom)
                .setParameter("rangeTo", rangeTo)
                .uniqueResult();
    }

    public static long getTotalSizeForMessage(Session session, String fiddId, long messageNumber) {
        Long sum = session.createQuery("SELECT sum(c.rangeTo - c.rangeFrom) FROM MetadataChunk c JOIN c.message m JOIN m.fidd f WHERE f.name = :fiddId AND m.number = :messageNumber", Long.class)
                .setParameter("fiddId", fiddId)
                .setParameter("messageNumber", messageNumber)
                .uniqueResult();
        return sum == null ? 0 : sum;
    }

    public static void removeOldestForMessage(Session session, String fiddId, long messageNumber) {
        MetadataChunk oldest = session.createQuery("SELECT c FROM MetadataChunk c JOIN c.message m JOIN m.fidd f WHERE f.name = :fiddId AND m.number = :messageNumber ORDER BY c.lastAccessTime ASC", MetadataChunk.class)
                .setParameter("fiddId", fiddId)
                .setParameter("messageNumber", messageNumber)
                .setMaxResults(1)
                .uniqueResult();
        if (oldest != null) {
            session.remove(oldest);
        }
    }
}
