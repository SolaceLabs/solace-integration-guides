package com.solace.sample;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

import com.solace.sample.Producer;

@TransactionManagement(value = TransactionManagementType.CONTAINER)
@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
/**
 * Message-Driven Bean implementation class for: ConsumerMDB
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "connectionFactoryJndiName", propertyValue = "JNDI/Sol/CF"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "JNDI/Sol/Q/requests") })
public class XAConsumerMDB implements MessageListener {

    @EJB(beanName = "XAProducerSB", beanInterface = Producer.class)
    Producer sb;

    /**
     * Default constructor.
     */
    public XAConsumerMDB() {
    }

    public void onMessage(Message message) {
        String msg = message.toString();

        System.out.println(Thread.currentThread().getName() + " - XAConsumerMDB: received message: " + msg);

        try {
            // Send reply message
            sb.sendMessage();
        } catch (JMSException e) {
            throw new EJBException("Error while sending reply message", e);
        }

        System.out.println("Completed processing!");

    }
}
