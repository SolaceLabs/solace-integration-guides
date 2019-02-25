package com.solace.sample;

import javax.ejb.Remote;
import javax.jms.JMSException;

@Remote
public interface Producer {

	public void sendMessage() throws JMSException;
}
