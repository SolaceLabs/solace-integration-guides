package com.solacesystems.demo;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;

/**
 * Simple JMS source function that consumes from a topic, translates the inbound messages
 * to the appropriate target output type using a JMSTranslator, then adds the translated value
 * to a SourceContext.
 *
 * @param <OUT> the target output type of JMS messages for internal consumption.
 */
public class JMSTopicSource<OUT> extends RichSourceFunction<OUT> implements ResultTypeQueryable<OUT> {

    /**
     * Constructor for the JMSTopicSource function. Parameters are cached but not used until the run() method is invoked.
     * @param jmsEnvironment the JMS environment properties for the InitialContext. This is used to lookup the connection-factory.
     * @param cfName the connection-factory name to retrieve from the JMS InitialContext and use for all connection creation.
     * @param topicName topic string to subscribe to; platform-dependent, will support whatever wildcarding the underlying platform supports.
     * @param deserializer a JMS translator instance that knows how to translate specific JMS messages to specific target types.
     */
    public JMSTopicSource(Hashtable<String,String> jmsEnvironment,
                          String cfName, String topicName,
                          JMSTranslator<OUT> deserializer) {
        _jmsEnv = jmsEnvironment;
        _cfName = cfName;
        _topicName = topicName;
        _deserializer = deserializer;
    }


    @Override
    public void run(SourceContext<OUT> sourceContext) throws Exception {
        InitialContext jndiContext = new InitialContext(_jmsEnv);
        ConnectionFactory factory = (ConnectionFactory) jndiContext.lookup(_cfName);
        Connection connection = factory.createConnection();
        Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        MessageConsumer consumer = session.createConsumer(session.createTopic(_topicName));
        connection.start();
        while(true) {
            Message msg = consumer.receive();
            sourceContext.collect(_deserializer.translate(msg));
            msg.acknowledge();
        }
    }

    @Override
    public void cancel() {
    }


    @Override
    public TypeInformation<OUT> getProducedType() {
        return TypeInformation.of(_deserializer.outputType());
    }

    private Hashtable<String, String> _jmsEnv;
    private JMSTranslator<OUT> _deserializer;
    private String _cfName;
    private String _topicName;
}
