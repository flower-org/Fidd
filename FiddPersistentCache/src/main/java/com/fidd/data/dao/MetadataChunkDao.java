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
}
