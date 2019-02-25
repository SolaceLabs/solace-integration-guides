package com.solace.sample;

import javax.ejb.Local;
import javax.jms.JMSException;

@Local
public interface ProducerLocal {

	public void sendMessage() throws JMSException;
}
