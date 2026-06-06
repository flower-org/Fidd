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
}
