package com.fidd.data.dao;

import com.fidd.data.model.Message;
import org.hibernate.Session;

public class MessageDao {
    public static void save(Session session, Message message) {
        session.persist(message);
    }

    public static Message update(Session session, Message message) {
        return session.merge(message);
    }

    public static void delete(Session session, Message message) {
        session.remove(message);
    }

    public static Message findByFiddIdAndMessageNumber(Session session, String fiddId, long messageNumber) {
        return session.createQuery("SELECT m FROM Message m JOIN m.fidd f WHERE f.name = :fiddId AND m.number = :messageNumber", Message.class)
                .setParameter("fiddId", fiddId)
                .setParameter("messageNumber", messageNumber)
                .uniqueResult();
    }

    public static long count(Session session) {
        return session.createQuery("SELECT count(m) FROM Message m", Long.class).uniqueResult();
    }

    public static void removeOldest(Session session) {
        Message oldest = session.createQuery("SELECT m FROM Message m ORDER BY m.lastAccessTime ASC", Message.class)
                .setMaxResults(1)
                .uniqueResult();
        if (oldest != null) {
            session.remove(oldest);
        }
    }

    public static void checkCapacityAndRemoveOldest(Session session, long capacity) {
        if (capacity > 0 && count(session) > capacity) {
            removeOldest(session);
        }
    }
}
