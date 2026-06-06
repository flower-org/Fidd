package com.fidd.data.dao;

import com.fidd.data.model.Fidd;
import org.hibernate.Session;

public class FiddDao {
    public static void save(Session session, Fidd fidd) {
        session.persist(fidd);
    }

    public static Fidd update(Session session, Fidd fidd) {
        return session.merge(fidd);
    }

    public static void delete(Session session, Fidd fidd) {
        session.remove(fidd);
    }
}
