package com.solace.sample;

import jakarta.ejb.Remote;
import jakarta.jms.JMSException;

@Remote
public interface Producer {

	public void sendMessage() throws JMSException;
}
