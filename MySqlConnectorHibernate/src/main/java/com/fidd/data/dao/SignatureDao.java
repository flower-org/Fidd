package com.fidd.data.dao;

import com.fidd.data.model.Signature;
import org.hibernate.Session;

public class SignatureDao {
    public static void save(Session session, Signature signature) {
        session.persist(signature);
    }

    public static Signature update(Session session, Signature signature) {
        return session.merge(signature);
    }

    public static void delete(Session session, Signature signature) {
        session.remove(signature);
    }

    public static Signature findByFiddIdMessageNumberTypeAndIndex(Session session, String fiddId, long messageNumber, com.fidd.data.model.SignatureType type, int index) {
        return session.createQuery("SELECT s FROM Signature s JOIN s.message m JOIN m.fidd f WHERE f.name = :fiddId AND m.number = :messageNumber AND s.type = :type AND s.index = :index", Signature.class)
                .setParameter("fiddId", fiddId)
                .setParameter("messageNumber", messageNumber)
                .setParameter("type", type)
                .setParameter("index", index)
                .uniqueResult();
    }

    public static long count(Session session, com.fidd.data.model.SignatureType type) {
        return session.createQuery("SELECT count(s) FROM Signature s WHERE s.type = :type", Long.class)
                .setParameter("type", type)
                .uniqueResult();
    }

    public static void removeOldest(Session session, com.fidd.data.model.SignatureType type) {
        Signature oldest = session.createQuery("SELECT s FROM Signature s WHERE s.type = :type ORDER BY s.lastAccessTime ASC", Signature.class)
                .setParameter("type", type)
                .setMaxResults(1)
                .uniqueResult();
        if (oldest != null) {
            session.remove(oldest);
        }
    }
}
