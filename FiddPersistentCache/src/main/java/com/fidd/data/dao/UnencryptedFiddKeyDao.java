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
}
