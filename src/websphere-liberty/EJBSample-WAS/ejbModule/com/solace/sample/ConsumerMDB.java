package com.solace.sample;

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

@MessageDriven
public class ConsumerMDB implements MessageListener {

    @EJB(beanName = "ProducerSB", beanInterface = Producer.class)
    Producer sb;

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
    }
}
