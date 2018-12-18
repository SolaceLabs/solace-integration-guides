package com.solace.sample;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import com.solace.sample.Producer;

@TransactionManagement(value = TransactionManagementType.BEAN)
@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)
/**
 * Message-Driven Bean implementation class for: ConsumerMDB
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "connectionFactoryJndiName", propertyValue = "JNDI/Sol/CF"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
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

        System.out.println(Thread.currentThread().getName() + " - ConsumerMDB: received message: " + msg);

        try {
            // Send reply message
            sb.sendMessage();
        } catch (JMSException e) {
            throw new EJBException("Error while sending reply message", e);
        }

        System.out.println("Completed processing!");

    }
}
