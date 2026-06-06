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
}
