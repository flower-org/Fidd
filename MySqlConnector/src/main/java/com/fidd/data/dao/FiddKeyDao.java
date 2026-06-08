package com.fidd.data.dao;

import com.fidd.data.model.FiddKey;
import org.hibernate.Session;

public class FiddKeyDao {
    public static void save(Session session, FiddKey fiddKey) {
        session.persist(fiddKey);
    }

    public static FiddKey update(Session session, FiddKey fiddKey) {
        return session.merge(fiddKey);
    }

    public static void delete(Session session, FiddKey fiddKey) {
        session.remove(fiddKey);
    }

    public static FiddKey findByFiddIdAndMessageNumberAndKeyName(Session session, String fiddId, long messageNumber, String keyName) {
        return session.createQuery("SELECT u FROM FiddKey u JOIN u.message m JOIN m.fidd f WHERE f.name = :fiddId AND m.number = :messageNumber AND u.keyName = :keyName", FiddKey.class)
                .setParameter("fiddId", fiddId)
                .setParameter("messageNumber", messageNumber)
                .setParameter("keyName", keyName)
                .uniqueResult();
    }

    public static long count(Session session) {
        return session.createQuery("SELECT count(u) FROM FiddKey u", Long.class).uniqueResult();
    }

    public static void removeOldest(Session session) {
        FiddKey oldest = session.createQuery("SELECT u FROM FiddKey u ORDER BY u.lastAccessTime ASC", FiddKey.class)
                .setMaxResults(1)
                .uniqueResult();
        if (oldest != null) {
            session.remove(oldest);
        }
    }
}
