package com.fidd.data.dao;

import com.fidd.data.model.FiddKeyCandidates;
import org.hibernate.Session;

public class FiddKeyCandidatesDao {
    public static void save(Session session, FiddKeyCandidates candidates) {
        session.persist(candidates);
    }

    public static FiddKeyCandidates update(Session session, FiddKeyCandidates candidates) {
        return session.merge(candidates);
    }

    public static void delete(Session session, FiddKeyCandidates candidates) {
        session.remove(candidates);
    }

    public static FiddKeyCandidates findByFiddIdMessageNumberAndFootprint(Session session, String fiddId, long messageNumber, String footprintBase64) {
        return session.createQuery("SELECT c FROM FiddKeyCandidates c JOIN c.message m JOIN m.fidd f WHERE f.name = :fiddId AND m.number = :messageNumber AND c.footprintBase64 = :footprintBase64", FiddKeyCandidates.class)
                .setParameter("fiddId", fiddId)
                .setParameter("messageNumber", messageNumber)
                .setParameter("footprintBase64", footprintBase64)
                .uniqueResult();
    }

    public static long count(Session session) {
        return session.createQuery("SELECT count(c) FROM FiddKeyCandidates c", Long.class).uniqueResult();
    }

    public static void removeOldest(Session session) {
        FiddKeyCandidates oldest = session.createQuery("SELECT c FROM FiddKeyCandidates c ORDER BY c.lastAccessTime ASC", FiddKeyCandidates.class)
                .setMaxResults(1)
                .uniqueResult();
        if (oldest != null) {
            session.remove(oldest);
        }
    }
}
