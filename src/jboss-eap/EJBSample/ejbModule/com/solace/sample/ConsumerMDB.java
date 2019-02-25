package com.solace.sample;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import com.solace.sample.Producer;

/**
 * Message-Driven Bean implementation class for: ConsumerMDB
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "connectionFactoryJndiName", propertyValue = "JNDI/Sol/CF"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "JNDI/Sol/Q/requests") })
public class ConsumerMDB implements MessageListener {

    @EJB(beanName = "ProducerSB", beanInterface = Producer.class)
    Producer sb;

    /**
     * Default constructor.
     */
    public ConsumerMDB() {
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
