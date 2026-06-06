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
}
