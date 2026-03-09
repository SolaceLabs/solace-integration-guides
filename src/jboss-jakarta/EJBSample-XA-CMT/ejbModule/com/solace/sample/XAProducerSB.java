package com.solace.sample;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
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

@Stateless(name = "XAProducerSB")
@TransactionManagement(value = TransactionManagementType.CONTAINER)
public class XAProducerSB implements Producer, ProducerLocal {

    @Resource(name = "myCF")
    ConnectionFactory myCF;

    @Resource(name = "myReplyQueue")
    Queue myReplyQueue;

    public XAProducerSB() {
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRED)
    @Override
    public void sendMessage() throws JMSException {

        Connection conn = null;
        Session session = null;
        MessageProducer prod = null;

        try {
            conn = myCF.createConnection();
            session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            prod = session.createProducer(myReplyQueue);

            ObjectMessage msg = session.createObjectMessage();
            msg.setObject("Hello world!");
            prod.send(msg, DeliveryMode.PERSISTENT, 0, 0);
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
