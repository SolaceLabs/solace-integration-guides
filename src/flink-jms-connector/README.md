# flink-jms-connector

A simple example JMS consumer library for Flink apps. Has basic 
`SourceFunction<OUT>` instances for JMS queues and topics.

The library is designed to allow you to plugin your own function to translate 
inbound JMS Message objects to a target type that is consumed by Flink. This 
is the bulk of the code you should have to write to use this library. 

The JMS code depends upon JNDI as an abstraction layer across JMS vendors 
so that this code can be used to integrate Flink with any JMS platform that 
supports standard JNDI resource lookups. The parameters passed into the 
`JMSTopicSourceFunction<OutType>` or `JMSQueueSourceFunction<OutType>` 
constructor are as follows:
- _jmsEnvironment_: a Hashtable containing the properties for creation of a 
JNDI `InitialContext` that is used for lookup and creation of all JMS resources
- _connectionFactoryName_: the JNDI name of the JMS connection-factory to 
be used to connect to the JMS provider
- _jmsProviderURL_: a connection string appropriate to the JMS you are 
connecting to
- _jmsUsername_: the username to authenticate to the JMS bus with if supported
- _jmsPassword_: the password for the JMS identity being used

Here's an example program:

```java
public class BasicTopicStreamingSample {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(4);

        final Hashtable<String, String> jmsEnv = new Hashtable<>();
        jmsEnv.put(InitialContext.INITIAL_CONTEXT_FACTORY, "com.solacesystems.jndi.SolJNDIInitialContextFactory");
        jmsEnv.put(InitialContext.PROVIDER_URL, "smf://192.168.56.101");
        jmsEnv.put(Context.SECURITY_PRINCIPAL, "test@test_vpn");
        jmsEnv.put(Context.SECURITY_CREDENTIALS, "password");

        env.addSource(new JMSTopicSource<String>(
                jmsEnv,
                "flink_cf",
                "flink/topic",
                new JMSTextTranslator())
        ).print();

        env.execute();
    }
```

## JMSTranslator\<OutputType\>

All inbound data is translated to your preferred format by a concrete 
`JMSTranslator` instance for JMS Messages. For a custom payload you should 
implement your own `JMSTranslator`. I elected not to use Flink's existing 
serialization approach as it only deals with primitive types and arrays, 
where you'd really prefer serialization from the full inbound message to 
access headers.

Here's an example translator that simply retrieves the text content from a 
JMS `TextMessage`; this is already provided as part of the flink-jms-connector 
library:

```java
public class JMSTextTranslator extends JMSTranslator<String> {
    @Override
    public String translate(Message msg) throws JMSException {
        TextMessage txtmsg = (TextMessage) msg;
        return txtmsg.getText();
    }
    @Override
    public Class<String> outputType() {
        return String.class;
    }
}

```