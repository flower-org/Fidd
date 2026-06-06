package com.fidd.data.dao;

import com.fidd.data.model.MessageSize;
import org.hibernate.Session;

public class MessageSizeDao {
    public static void save(Session session, MessageSize messageSize) {
        session.persist(messageSize);
    }

    public static MessageSize update(Session session, MessageSize messageSize) {
        return session.merge(messageSize);
    }

    public static void delete(Session session, MessageSize messageSize) {
        session.remove(messageSize);
    }
}
