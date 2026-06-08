package com.fidd.data.dao;

import com.fidd.data.model.UnencryptedFiddKey;
import org.hibernate.Session;

public class UnencryptedFiddKeyDao {
    public static void save(Session session, UnencryptedFiddKey unencryptedFiddKey) {
        session.persist(unencryptedFiddKey);
    }

    public static UnencryptedFiddKey update(Session session, UnencryptedFiddKey unencryptedFiddKey) {
        return session.merge(unencryptedFiddKey);
    }

    public static void delete(Session session, UnencryptedFiddKey unencryptedFiddKey) {
        session.remove(unencryptedFiddKey);
    }

    public static UnencryptedFiddKey findByFiddIdAndMessageNumber(Session session, String fiddId, long messageNumber) {
        return session.createQuery("SELECT u FROM UnencryptedFiddKey u JOIN u.message m JOIN m.fidd f WHERE f.name = :fiddId AND m.number = :messageNumber", UnencryptedFiddKey.class)
                .setParameter("fiddId", fiddId)
                .setParameter("messageNumber", messageNumber)
                .uniqueResult();
    }

    public static long count(Session session) {
        return session.createQuery("SELECT count(u) FROM UnencryptedFiddKey u", Long.class).uniqueResult();
    }

    public static void removeOldest(Session session) {
        UnencryptedFiddKey oldest = session.createQuery("SELECT u FROM UnencryptedFiddKey u ORDER BY u.lastAccessTime ASC", UnencryptedFiddKey.class)
                .setMaxResults(1)
                .uniqueResult();
        if (oldest != null) {
            session.remove(oldest);
        }
    }
}
