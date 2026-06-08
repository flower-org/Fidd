package com.fidd.data;

import com.fidd.data.model.Fidd;
import com.fidd.data.model.Message;
import com.fidd.data.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.ArrayList;

public class DemoApp {
    public static void main(String[] args) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();

        try {
            // Create a Fidd
            Fidd fidd = new Fidd();
            fidd.setName("TestFidd");

            // Create a Message
            Message msg = new Message();
            msg.setNumber(1L);
            msg.setFidd(fidd);
            msg.setMetadataRange("0-100");

            fidd.setMessages(new ArrayList<>());
            fidd.getMessages().add(msg);

            // Save Fidd (cascades to Message)
            session.persist(fidd);

            tx.commit();
            System.out.println("Demo Fidd and Message created successfully!");
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
            HibernateUtil.shutdown();
        }
    }
}
