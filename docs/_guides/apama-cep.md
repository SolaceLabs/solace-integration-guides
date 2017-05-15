# apama-solace-sample
Sample Apama project and Java project to demonstrate JMS connection from Solace to Apama using Apama's Correlator-Integrated Messaging for JMS.

![Sequence Diagram](resources/solace2apama.png)

Apama CEP has been used in many real-time applications, i.e. HFT, trade surveillance, auto hedging, real-time betting, real-time analytics, real-time firewall and etc. 

Solace's ultra wide bandwidth and low latency messaging router works well with Apama hand-in-hand.

##Solace JCSMP project
- Create a session independent TextMessage - In a Session independent message ownership model, client applications can reuse messages between send operations. Messages are allocated on demand and are disposed explicitly by client applications when they are done with the messages.

        TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);

- Create a Structured Data Map for message properties

        SDTMap map = prod.createMap();

- Add Apama event name "com.solace.sample.SampleTextMessage" in MESSAGE_TYPE

        map.putString("MESSAGE_TYPE", "com.solace.sample.SampleTextMessage");
        
- Add additional message properties
 
        map.putLong("MESSAGE_CREATED", System.currentTimeMillis());
        
- Add message property to message

        msg.setProperties(map);

- Send off the message to a topic destination

        prod.send(msg, topic);

```java

    SDTMap map = prod.createMap();
    map.putString("MESSAGE_TYPE", "com.solace.sample.SampleTextMessage");
    TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
    for (int msgsSent = 0; msgsSent < 1000000; ++msgsSent) {

        msg.reset();
    
        msg.setText("msg count is " + String.format("%05d", msgsSent));
        msg.setDeliveryMode(DeliveryMode.DIRECT);
        msg.setApplicationMessageId("appID-" + msgsSent);
        msg.setElidingEligible(true);
        map.putLong("MESSAGE_CREATED", System.currentTimeMillis());
    
        msg.setProperties(map);
        prod.send(msg, topic);
        Thread.sleep(500);
    }
```

##Solace router configuration

**Message VPN creation**

This section outlines how to create a message-VPN called "apama" on the Solace Message Router with authentication disabled and 200MB of message spool quota for Guaranteed Messaging. This VPN name is required in the Apama configuration when connecting to the Solace message router. Actual values for authentication, message spool and other message-VPN properties may vary depending on the end application use cases.

```text
(config)# create message-vpn apama
(config-msg-vpn)# authentication
(config-msg-vpn-auth)# user-class client
(config-msg-vpn-auth-user-class)# basic auth-type none
(config-msg-vpn-auth-user-class)# exit
(config-msg-vpn-auth)# exit
(config-msg-vpn)# no shutdown
(config-msg-vpn)# exit
(config)#
(config)# message-spool message-vpn apama
(config-message-spool)# max-spool-usage 200
(config-message-spool)# exit
(config)#
```

**Solace JNDI references creation**

To enable the JMS clients to connect required by Apama, there is one JNDI object required on the Solace Message Router:

- A connection factory: /jms/cf/apama

```text
(config)# jndi message-vpn apama
(config-jndi)# create connection-factory /jms/cf/apama
(config-jndi-connection-factory)# property-list messaging-properties
(config-jndi-connection-factory-pl)# property default-delivery-mode persistent
(config-jndi-connection-factory-pl)# exit
(config-jndi-connection-factory)# property-list transport-properties
(config-jndi-connection-factory-pl)# property direct-transport false
(config-jndi-connection-factory-pl)# property "reconnect-retry-wait" "3000"
(config-jndi-connection-factory-pl)# property "reconnect-retries" "3"
(config-jndi-connection-factory-pl)# property "connect-retries-per-host" "5"
(config-jndi-connection-factory-pl)# property "connect-retries" "1"
(config-jndi-connection-factory-pl)# exit
(config-jndi-connection-factory)# exit
(config-jndi)# no shutdown
(config-jndi)# exit
(config)#
```

**Client Usernames & Profiles creation**

This section outlines how to update the default client-profile and how to create a client username for connecting to the Solace Message Router. For the client-profile, it is important to enable guaranteed messaging for JMS messaging, endpoint creation and transacted sessions if using transactions.

The test client username of "apama_user" will be required by the Apama when connecting to the Solace Message Router.

```text
(config)# client-profile default message-vpn apama
(config-client-profile)# message-spool allow-guaranteed-message-receive
(config-client-profile)# message-spool allow-guaranteed-message-send
(config-client-profile)# message-spool allow-guaranteed-endpoint-create
(config-client-profile)# message-spool allow-transacted-sessions
(config-client-profile)# exit
(config)#
(config)# create client-username apama_user message-vpn apama
(config-client-username)# acl-profile default   
(config-client-username)# client-profile default
(config-client-username)# no shutdown
(config-client-username)# exit
(config)#
```

##Apama CEP configuration

**Apama JMS configuration**

The Apama correlator-integrated messaging for JMS configuration consists of a set of XML files and .properties files.

A correlator that supports JMS has the following two files:

- jms-global-spring.xml
- jms-mapping-spring.xml

In addition, for each JMS connection added to the configuration, there will be an additional XML and .properties file :

- connectionId-spring.xml
- connectionId-spring.properties

The JMS property file has all the custom settings and values in each use case.

```properties
connectionFactory.jndiName.solace=/jms/cf/apama
jndiContext.environment.solace=java.naming.factory.initial\=com.solacesystems.jndi.SolJNDIInitialContextFactory\njava.naming.provider.url\=${jndiContext.environment.provider.url.solace}\n
jndiContext.environment.provider.url.solace=smf\://192.168.4.167
jndiContext.jndiAuthentication.username.solace=apama_user@apama
jndiContext.jndiAuthentication.password.solace=
connectionAuthentication.username.solace=
connectionAuthentication.password.solace=
clientId.solace=
staticReceiverList.solace=topic\:apamaTopic;
defaultReceiverReliability.solace=BEST_EFFORT
defaultSenderReliability.solace=BEST_EFFORT
JMSProviderInstallDir.solace=C\:/tools/SoftwareAG/common/lib
classpath.solace=libs/sol-common-10.0.1.jar;libs/sol-jcsmp-10.0.1.jar;libs/sol-jms-10.0.1.jar;
```
The message to event mapping is done in adapter editor. Manual changes to the mapping XML should be avoided.

- JMS body is mapped to payload field
- JMS property is mapped to extraParam field

```xml
<mapping:mapOutput>
    <mapping:property name="eventType" value="com.solace.sample.SampleTextMessage"/>
    <mapping:rule>
      <mapping:source><![CDATA[${jms.body.textmessage}]]></mapping:source>
      <mapping:target><![CDATA[${apamaEvent['payload']}]]></mapping:target>
      <mapping:action><![CDATA[None]]></mapping:action>
      <mapping:type><![CDATA[BINDING_PARAM]]></mapping:type>
    </mapping:rule>
    <mapping:rule>
      <mapping:source><![CDATA[${jms.properties}]]></mapping:source>
      <mapping:target><![CDATA[${apamaEvent['extraParam']}]]></mapping:target>
      <mapping:action><![CDATA[None]]></mapping:action>
      <mapping:type><![CDATA[BINDING_PARAM]]></mapping:type>
    </mapping:rule>
  </mapping:mapOutput>
```

**Apama event definition**

```go
event SampleTextMessage {
    string payload;
    dictionary<string, string> extraParam;
}
```

**Apama Event Processing Language source**

```go
monitor SampleTopicReceiver {
    action getSolaceMessage() {
        on AppStarted() {
            JMS.onApplicationInitialized();
        }
    }
    action onload () {
        getSolaceMessage();
        on JMSReceiverStatus() as receiverStatus
        {
            log "Received receiverStatus from JMS: " + receiverStatus.toString() at INFO;
            on all SampleTextMessage() as sampleTextMessage {
                log "From JMS: " + sampleTextMessage.toString() at INFO;
            }
        }
        route AppStarted();
    }
}
```

##Debugging Tips for Apama JMS Integration

Application logs have the most runtime informaiton. Solace Java API integrates with log4j library. Apama logging is dynamic as verbosity can be tuned up and down at runtime.

**Solace logging**

By default INFO leve logs will be written to the console. This section will focus on using log4j as the logging library and tuning Solace API logs using the log4j properties. Therefore in order to enable Solace JMS API logging, a user must do two things:

- Put Log4j on the classpath
- Create a log4j.properties configuration file in the root folder of the classpath

Below is an example Log4j properties file that will enable debug logging within the Solace API.

```properties
log4j.rootCategory=INFO, stdout
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%d{ABSOLUTE} %5p %t %c{2}:%L - %m%n
log4j.category.com.solacesystems.jms=DEBUG
log4j.category.com.solacesystems.jcsmp=DEBUG
```

Solace console logs show received messages from the messaging router

```text
Received message: 
Destination:                            Topic 'apamaTopic'
AppMessageID:                           appID-8224
Priority:                               0
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             8230
Eliding Eligible
User Property Map:                      2 entries
  Key 'MESSAGE_CREATED' (Long): 1488757225657
  Key 'MESSAGE_TYPE' (String): com.solace.sample.SampleTextMessage

Binary Attachment:                      len=21
  1c 15 6d 73 67 20 63 6f    75 6e 74 20 69 73 20 30    ..msg.count.is.0
  38 32 32 34 00                                        8224.

msg count is 08224
```

**Apama logging**

Some of the JMS sender/receiver properties can be toggled during development to debug application and exam JMS messages and performance matrix
 
```xml
<bean id="globalReceiverSettings" class="com.apama.correlator.jms.config.JmsReceiverSettings">
        
  <property name="receiverFlowControl" value="false"/>

  <!-- These logging options are for testing/diagnostics only and should 
    not be enabled in a production system due to the possible 
    performance impact 
  -->
  <property name="logJmsMessages" value="false"/>
  <property name="logJmsMessageBodies" value="true"/>
  <property name="logProductMessages" value="false"/>

  <property name="logPerformanceBreakdown" value="true"/>
  <property name="logDetailedStatus" value="false"/>
</bean>
```

Log output shows JMS performance breakdown details, correlator status, JMS status and received JMS message

```text
2017-03-05 14:20:30.738 INFO  [23732:JMSReceiver:solace-receiver-apamaTopic] -      40% RECEIVING:    min,mean,max secs per message = 0.193150, 0.199414, 0.206460
2017-03-05 14:20:30.738 INFO  [23732:JMSReceiver:solace-receiver-apamaTopic] -       0% MAPPING:      min,mean,max secs per message = 0.000385, 0.000874, 0.001937
2017-03-05 14:20:30.738 INFO  [23732:JMSReceiver:solace-receiver-apamaTopic] -       0% ENQUEUING:    min,mean,max secs per batch = 0.000118, 0.000296, 0.000484
2017-03-05 14:20:30.738 INFO  [23732:JMSReceiver:solace-receiver-apamaTopic] -       0% JMS_ACK:      min,mean,max secs per batch = 0.000000, 0.000002, 0.000003
2017-03-05 14:20:30.738 INFO  [23732:JMSReceiver:solace-receiver-apamaTopic] -      60% R_TIMEOUTS:   min,mean,max secs per batch = 0.299299, 0.300074, 0.300875
2017-03-05 14:20:30.738 INFO  [23732:JMSReceiver:solace-receiver-apamaTopic] -     100% TOTAL:        min,mean,max secs per batch = 0.494446, 0.500696, 0.507113
...
2017-03-05 15:56:24.481 INFO  [19636] - Correlator Status: sm=3 nctx=1 ls=11 rq=0 eq=0 iq=0 oq=0 icq=0 lcn="<none>" lcq=0 lct=0.0 rx=6187 tx=0 rt=1 nc=1 vm=494716 pm=23020 runq=0 si=9.0 so=0.0 srn="<none>" srq=0 (no license file)
2017-03-05 15:56:24.484 INFO  [19636:Status] - JMS Status: s=1 tx=0 sRate=0 sOutst=0 r=1 rx=6,182 rRate=0 rWindow=0 rRedel=0 rMaxDeliverySecs=0.0 rDupsDet=0 rDupIds=0 connErr=0 jvmMB=63
...
2017-03-05 18:17:36.091 INFO  [27192:JMSReceiver:solace-receiver-apamaTopic] - 'solace-receiver-apamaTopic' has received JMS message: Property.JMS_Solace_DeadMsgQueueEligible=false
   Property.JMS_Solace_DeliverToOne=false
   Property.JMS_Solace_ElidingEligible=false
   Property.JMS_Solace_isXML=false
   Property.MESSAGE_CREATED=1488755856084
   Property.MESSAGE_TYPE=com.solace.sample.SampleTextMessage
   Property.Solace_JMS_Prop_IS_Reply_Message=false
   JMSDestination=Topic<apamaTopic>
   JMSMessageID=appID-5488
   JMSRedelivered=false
   JMSTimestamp=0
   JMSTimestamp.toString=0
   JMSTimestamp.approxAgeInMillis=N/A
   JMSExpiration=0
   JMSExpiration.toString=0
   JMSReplyTo=<NullDestination>
   JMSCorrelationID=null
   JMSDeliveryMode=NON_PERSISTENT
   JMSPriority=0
   MessageClass=TextMessage
   Body="msg count is 05488"
2017-03-05 18:17:36.092 INFO  [33608] - com.solace.sample.SampleTopicReceiver [3] From JMS: com.solace.sample.SampleTextMessage("msg count is 05488",{"JMS_Solace_DeadMsgQueueEligible":"false","JMS_Solace_DeliverToOne":"false","JMS_Solace_ElidingEligible":"false","JMS_Solace_isXML":"false","MESSAGE_CREATED":"1488755856084","MESSAGE_TYPE":"com.solace.sample.SampleTextMessage","Solace_JMS_Prop_IS_Reply_Message":"false"})
```


## License

This project is licensed under the Apache License, Version 2.0. - See the [LICENSE](LICENSE) file for details.


## Resources

For more information try these resources:

- The Solace Developer Portal website at: http://dev.solace.com
- Get a better understanding of [Solace technology](http://dev.solace.com/tech/).
- Check out the [Solace blog](http://dev.solace.com/blog/) for other interesting discussions around Solace technology
- Ask the [Solace community.](http://dev.solace.com/community/)
- Software AG Apama Community website at: http://techcommunity.softwareag.com/ecosystem/communities/public/apama/contents/downloads-all/
