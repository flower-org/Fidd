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
}
