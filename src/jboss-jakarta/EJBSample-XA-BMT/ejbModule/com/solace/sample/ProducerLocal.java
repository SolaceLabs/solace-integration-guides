package com.solace.sample;

import jakarta.ejb.Local;
import jakarta.jms.JMSException;

@Local
public interface ProducerLocal {

	public void sendMessage() throws JMSException;
}
