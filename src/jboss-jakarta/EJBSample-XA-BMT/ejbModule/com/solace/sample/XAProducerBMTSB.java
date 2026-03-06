package com.solace.sample;

import jakarta.annotation.Resource;
import jakarta.ejb.EJBException;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.transaction.UserTransaction;

@Stateless(name = "XAProducerBMTSB")
@TransactionManagement(value=TransactionManagementType.BEAN)

public class XAProducerBMTSB implements Producer, ProducerLocal {
    @Resource(name = "myCF")
    ConnectionFactory myCF;

    @Resource(name = "myReplyQueue")
    Queue myReplyQueue;

    @Resource
    SessionContext sessionContext;

    public XAProducerBMTSB() {
    }

    @Override
    public void sendMessage() throws JMSException {

        System.out.println("Sending reply message");
        Connection conn = null;
        Session session = null;
        MessageProducer prod = null;
        UserTransaction ux = sessionContext.getUserTransaction();

        try {
            ux.begin();
            conn = myCF.createConnection();
            session = conn.createSession(true, Session.AUTO_ACKNOWLEDGE);
            prod = session.createProducer(myReplyQueue);
            ObjectMessage msg = session.createObjectMessage();
            msg.setObject("Hello world!");
            prod.send(msg, DeliveryMode.PERSISTENT, 0, 0);
            ux.commit();
        } catch (Exception e) {
            e.printStackTrace();
            try {
               ux.rollback();
            } catch (Exception ex) {
               throw new EJBException(
                "rollback failed: " + ex.getMessage(), ex);
            }
        } finally {
            if (prod != null)
          prod.close();
            if (session != null)
          session.close();
            if (conn != null)
          conn.close();
        }
    }
}
