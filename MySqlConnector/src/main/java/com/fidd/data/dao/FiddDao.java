package com.fidd.data.dao;

import com.fidd.data.model.Fidd;
import org.hibernate.Session;
import java.util.Arrays;
import java.util.List;

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

    public static Fidd findById(Session session, java.util.UUID id) {
        return session.find(Fidd.class, id);
    }

    public static Fidd findByName(Session session, String name) {
        return session.createQuery("SELECT f FROM Fidd f WHERE f.name = :name", Fidd.class)
                .setParameter("name", name)
                .uniqueResult();
    }

    public static List<Fidd> search(Session session, String searchPhrase) {
        if (searchPhrase == null || searchPhrase.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // Split phrase into individual words for tags and name matching
        List<String> words = Arrays.asList(searchPhrase.trim().split("\\s+"));

        StringBuilder nameClauses = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            if (i > 0) nameClauses.append(" AND ");
            nameClauses.append("f.name LIKE :w").append(i);
        }

        var query = session.createNativeQuery(
                "SELECT DISTINCT f.* FROM fidd f " +
                "LEFT JOIN fidd_tags t ON f.id = t.fidd_id " +
                "WHERE (" + nameClauses + ") " +
                "OR MATCH(f.description) AGAINST(:phrase IN NATURAL LANGUAGE MODE) " +
                "OR t.tag IN (:words)", Fidd.class);

        for (int i = 0; i < words.size(); i++) {
            query.setParameter("w" + i, "%" + words.get(i) + "%");
        }
        query.setParameter("phrase", searchPhrase);
        query.setParameter("words", words);

        return query.getResultList();
    }
}
