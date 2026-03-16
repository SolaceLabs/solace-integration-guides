package com.solace.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.receiver.Receiver;
import com.solacesystems.jms.SupportedProperty;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;

public class PubSubPlusJMSReceiver extends Receiver<String> implements MessageListener {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private static final Logger log = LoggerFactory.getLogger(PubSubPlusJMSReceiver.class);
  private static final String SOLJMS_INITIAL_CONTEXT_FACTORY = "com.solacesystems.jndi.SolJNDIInitialContextFactory";
  private String _brokerURL;
  private String _vpn;
  private String _username;
  private String _password;
  private String _queueName;
  private String _connectionFactory;
  private Connection _connection;

  public PubSubPlusJMSReceiver(String brokerURL,
                               String vpn,
                               String username,
                               String password,
                               String jndiQueueName,
                               String jndiConnectionFactory,
                               StorageLevel storageLevel)
  {
    super(storageLevel);
    _brokerURL = brokerURL;
    _vpn = vpn;
    _username = username;
    _password = password;
    _queueName = jndiQueueName;
    _connectionFactory = jndiConnectionFactory;
  }

  @Override
  public void onStart()
  {
    log.info("Starting up...");
    try
    {
      Hashtable<String, String> env = new Hashtable<String, String>();
      env.put(InitialContext.INITIAL_CONTEXT_FACTORY,
          SOLJMS_INITIAL_CONTEXT_FACTORY);
      env.put(InitialContext.PROVIDER_URL, _brokerURL);
      env.put(Context.SECURITY_PRINCIPAL, _username);
      env.put(Context.SECURITY_CREDENTIALS, _password);
      env.put(SupportedProperty.SOLACE_JMS_VPN, _vpn);
      javax.naming.Context context = new javax.naming.InitialContext(env);
      ConnectionFactory factory = (ConnectionFactory)
          context.lookup(_connectionFactory);
      Destination queue = (Destination) context.lookup(_queueName);
      _connection = factory.createConnection();
      _connection.setExceptionListener(new JMSReceiverExceptionListener());
      Session session = _connection.createSession(false,
          Session.CLIENT_ACKNOWLEDGE);

      MessageConsumer consumer;
      consumer = session.createConsumer(queue);
      consumer.setMessageListener(this);
      _connection.start();
      log.info("Completed startup.");
    } catch (Exception ex) {
      // Caught exception, try a restart
      log.error("Callback onStart caught exception, restarting ", ex);
      restart("Callback onStart caught exception, restarting ", ex);
    }
  }

  @Override
  public void onStop() {
    log.info("Callback onStop called");
    try {
      _connection.close();
    } catch (JMSException ex) {
      log.error("onStop exception", ex);
    }
  }

  @Override
  public void onMessage(Message message) {
    log.info("Callback onMessage received" + message);
    store(message.toString());
    try {
      message.acknowledge();
    } catch (JMSException ex) {
      log.error("Callback onMessage failed to ack message", ex);
    }
  }

  private class JMSReceiverExceptionListener implements ExceptionListener
  {

    @Override
    public void onException(JMSException ex) {
      log.error("JMS exceptionListener caught exception, , restarting ", ex);
      restart("JMS exceptionListener caught exception, , restarting ");
    }
  }

  @Override
  public String toString() {
    return "PubSubPlusJMSReceiver{" + "brokerURL='" + _brokerURL + '\'' + ", vpn='" + _vpn + '\'' + ", username='" + _username
        + '\'' + ", queueName='" + _queueName + '\'' + ", connectionFactory='" + _connectionFactory + '\'' + '}';
  }
}