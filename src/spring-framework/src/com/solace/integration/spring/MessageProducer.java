package com.solace.integration.spring;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

public class MessageProducer {
  private JmsTemplate jmsTemplate;

  public void sendMessages(String messagetext) throws JMSException {
    getJmsTemplate().send(new MessageCreator() {
      public Message createMessage(Session session) throws JMSException {
        Message message = session.createTextMessage(messagetext);
        return message;
      }
    });
  }

  public JmsTemplate getJmsTemplate() {
    return jmsTemplate;
  }

  public void setJmsTemplate(JmsTemplate jmsTemplate) {
    this.jmsTemplate = jmsTemplate;
  }

  public static void main(String[] args) throws JMSException {
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
        new String[] { "SolResources.xml" });
    MessageProducer producer = (MessageProducer) context.getBean("messageProducer");
    for (int i = 0; i < 10; i++) {
      String messagetext = "Test#" + i;
      System.out.println("Sending message " + messagetext);
      producer.sendMessages(messagetext);
    }
//    context.close();	// Not closing here because waiting for MessageConsumer to receive all the messages
  }
}
