---
layout: guides
title: Apache Flink Streaming 2.10
summary: Apache Flink provides an optimized engine that supports distributed, high-performing, always-available, and accurate data streaming applications. Flink Streaming supports high-throughput, fault-tolerant stream processing of live data streams for continuous processing of unbounded datasets.  The Flink Streaming generic SourceFunction is a simple interface that allows third party applications to push data into Flink in an efficient manner. 
icon: flink_squirrel.png
links:
    - label: Example Source Code - Flink JMS Connector
      link: https://github.com/SolaceLabs/solace-integration-guides/blob/master/src/flink-jms-connector
---

## Overview

This document demonstrates how to integrate the Solace Java Message Service (JMS) with Flink Streaming source functions for consumption of JMS messages. The goal of this document is to outline best practices for this integration to enable efficient use of both Flink Streaming and Solace JMS. 

The target audience of this document is developers with knowledge of both Spark and JMS in general. As such this document focuses on the technical steps required to achieve the integration. For detailed background on either Solace JMS or Flink refer to the referenced documents below.

This document is divided into the following sections to cover the Solace JMS integration with Flink Streaming:

* Integrating with Flink Streaming
* Working with Solace High Availability
* Debugging Tips 


### Related Documentation

These links contain information related to this guide:

* [Solace Developer Portal]({{ site.links-dev-portal }}){:target="_top"}
* [Solace Messaging API for JMS]({{ site.links-docs-jms }}){:target="_top"}
* [Solace JMS API Online Reference Documentation]({{ site.links-docs-jms-api }}){:target="_top"}
* [Solace Feature Guide]({{ site.links-docs-features }}){:target="_top"}
* [Solace Message Router Configuration]({{ site.links-docs-router-config }}){:target="_top"}
* [Solace Command Line Interface Reference]({{ site.links-docs-cli }}){:target="_top"}
* [Flink Streaming Documentation](https://ci.apache.org/projects/flink/flink-docs-release-1.2/dev/datastream_api.html){:target="_blank"}
* [Spark SourceFunction Class Documentation](https://ci.apache.org/projects/flink/flink-docs-master/api/java/org/apache/flink/streaming/api/functions/source/SourceFunction.html){:target="_blank"}

## Integrating with Flink Streaming
This is a discussion of an approach for consuming messages from a Java Messaging Service (JMS) bus in Flink containers. The full code is freely available on Github as part of this project in [src/flink-jms-connector]({{ site.repository }}/blob/master/src/flink-jms-connector){:target="_blank"}.

The general Flink Streaming support for connectors is documented in the [Flink Streaming Documentation](https://ci.apache.org/projects/flink/flink-docs-release-1.2/dev/datastream_api.html){:target="_blank"}. The configuration outlined in this document makes use of a custom SourceFunction to achieve the desired integration with Solace via JMS. 

This integration guide demonstrates how to configure a Flink Streaming application to receive JMS messages using a custom receiver. Accomplishing this requires completion of the following steps. 

* Step 1 - Obtain access to Solace message router and JMS API, see the [Solace Developer Portal]({{ site.links-dev-portal }}){:target="_top"}
* Step 2 - Configuration of the Solace Message Router.
* Step 3 - Coding a Flink JMS SourceFunction.
* Step 4 - Deploying Flink JMS SourceFunction.

### Description of Resources Required

This integration guide will demonstrate creation of Solace JMS custom receiver and configuring the receiver to receive messages. This section outlines the resources that are required/created and used in the subsequent sections.

#### Solace Resources

The following Solace Message Router resources are required.


<table>
    <tr>
    <th>Resource</th>
    <th>Value</th>
    <th>Description</th>
    </tr>
    <tr>
    <td>Solace Message Router IP:Port</td>
    <td>__IP:Port__</td>
    <td>The IP address and port of the Solace appliance message backbone. This is the address client’s use when connecting to the Solace appliance to send and receive message. This document uses a value of __IP:PORT__.</td>
    </tr>
    <tr>
    <td>Message VPN</td>
    <td>Solace_Flink_VPN</td>
    <td>A Message VPN, or virtual message broker, to scope the integration on the Solace message router.</td>
    </tr>
    <tr>
    <td>Client Username</td>
    <td>flink_user</td>
    <td>The client username.</td>
    </tr>
    <tr>
    <td>Client Password</td>
    <td>flink_password</td>
    <td>Optional client password. </td>
    </tr>
    <tr>
    <td>Solace Queue</td>
    <td>Q/receiver</td>
    <td>Solace destination of persistent messages consumed</td>
    </tr>
    <tr>
    <td>JNDI Connection Factory</td>
    <td>JNDI/CF/flink</td>
    <td>The JNDI Connection factory for controlling Solace JMS connection properties</td>
    </tr>
    <tr>
    <td>JNDI Queue Name</td>
    <td>JNDI/Q/receiver</td>
    <td>The JNDI name of the queue used in the samples</td>
    </tr>
</table>

###	Step 1 – Obtain access to Solace message router and JMS API

The Solace messaging router can be obtained one of 2 ways.     
1.	If you are in an organization that is an existing Solace customer, it is likely your organization already has Solace Message Routers and corporate policies about their use.  You will have to contact your middleware operational team in regards to access to a Solace Message Router.
2.	If you are new to Solace or your company does not have development message routers, you can obtain a trail Solace Virtual Message Router (VMR) from the [Solace Developer Portal Downloads]({{ site.links-downloads }}){:target="_top"}. For help getting started with your Solace VMR you can refer to [Solace VMR Getting Started Guides]({{ site.links-vmr-getstarted }}){:target="_top"}.

The Solace JMS are required.  They can be obtained on [Solace Developer Portal Downloads]({{ site.links-downloads }}){:target="_top"} or from [Maven Central]({{ site.links-jms-maven }}){:target="_blank"}.

<table>
    <tr>
    <th>Resource</th>
    <th>Value</th>
    <th>Description</th>
    </tr>
    <tr>
    <td>Solace Common</td>
    <td>sol-common-VERSION.jar</td>
    <td>Solace common utilities library.</td>
    </tr>
    <tr>
    <td>Solace JCSMP</td>
    <td>sol-jcsmp-VERSION.jar</td>
    <td>Underlying Solace wireline support libraries.</td>
    </tr>
    <tr>
    <td>Solace JMS</td>
    <td>sol-jms-VERSION.jar</td>
    <td>Solace JMS 1.1 compliant libraries.</td>
    </tr>
    <tr>
    <td>Apache Commons language</td>
    <td>commons-lang-2.6.jar</td>
    <td>Common language libraries. </td>
    </tr>
    <tr>
    <td>Apache Commons logging</td>
    <td>commons-logging-1.1.3.jar</td>
    <td>Common logging libraries</td>
    </tr>
    <tr>
    <td>Apache Geronimo</td>
    <td>geronimo-jms_1.1_spec-1.1.1.jar</td>
    <td>Apache Geronimo is an open source server runtime that integrates the best open source projects to create Java/OSGi server runtimes that meet the needs of enterprise developers and system administrators. Our most popular distribution is a fully certified Java EE 6 application server runtime.</td>
    </tr>
</table>

### Step 2 – Configuring the Solace Message Router

The Solace appliance needs to be configured with the following configuration objects at a minimum to enable JMS to send and receive messages within the Spark application. 

* A Message VPN, or virtual message broker, to scope the integration on the Solace appliance.
* Client connectivity configurations like usernames and profiles
* Guaranteed messaging endpoints for receiving messages.
* Appropriate JNDI mappings enabling JMS clients to connect to the Solace appliance configuration.

For reference, the CLI commands in the following sections are from SolOS version 6.2 but will generally be forward compatible. For more details related to Solace appliance CLI see [Solace Command Line Interface Reference]({{ site.links-docs-cli }}){:target="_top"}. Wherever possible, default values will be used to minimize the required configuration. The CLI commands listed also assume that the CLI user has a Global Access Level set to Admin. For details on CLI access levels please see [Solace Feature Guide]({{ site.links-docs-features }}){:target="_top"} section “User Authentication and Authorization”.

Also note that this configuration can also be easily performed using SolAdmin, Solace’s GUI management tool. This is in fact the recommended approach for configuring a Solace appliance. This document uses CLI as the reference to remain concise.

#### Creating a Message VPN

This section outlines how to create a message-VPN called “Solace_Flink_VPN” on the Solace appliance with authentication disabled and 2GB of message spool quota for Guaranteed Messaging. This message-VPN name is required in the Flink configuration when connecting to the Solace messaging appliance. In practice appropriate values for authentication, message spool and other message-VPN properties should be chosen depending on the end application’s use case. 

```
(config)# create message-vpn Solace_Flink_VPN
(config-msg-vpn)# authentication
(config-msg-vpn-auth)# user-class client
(config-msg-vpn-auth-user-class)# basic auth-type none
(config-msg-vpn-auth-user-class)# exit
(config-msg-vpn-auth)# exit
(config-msg-vpn)# no shutdown
(config-msg-vpn)# exit
(config)#
(config)# message-spool message-vpn Solace_Flink_VPN
(config-message-spool)# max-spool-usage 2000
(config-message-spool)# exit
(config)#
```

#### Configuring Client Usernames & Profiles

This section outlines how to update the default client-profile and how to create a client username for connecting to the Solace appliance. For the client-profile, it is important to enable guaranteed messaging for JMS messaging and transacted sessions if using transactions.

The chosen client username of “flink_user” will be required by the Flink application when connecting to the Solace appliance.

```
(config)# client-profile default message-vpn Solace_Flink_VPN
(config-client-profile)# message-spool allow-guaranteed-message-receive
(config-client-profile)# message-spool allow-guaranteed-message-send
(config-client-profile)# message-spool allow-transacted-sessions
(config-client-profile)# exit
(config)#
(config)# create client-username flink_user message-vpn Solace_Flink_VPN
(config-client-username)# acl-profile default	
(config-client-username)# client-profile default
(config-client-username)# no shutdown
(config-client-username)# exit
(config)#
```

#### Setting up Guaranteed Messaging Endpoints
This integration guide shows receiving messages within the Flink application from a single JMS Queue. For illustration purposes, this queue is chosen to be an exclusive queue with a message spool quota of 2GB matching quota associated with the message VPN. The queue name chosen is “Q/requests”.

```
(config)# message-spool message-vpn Solace_Flink_VPN
(config-message-spool)# create queue Q/receive
(config-message-spool-queue)# access-type exclusive
(config-message-spool-queue)# max-spool-usage 2000
(config-message-spool-queue)# permission all delete
(config-message-spool-queue)# no shutdown
(config-message-spool-queue)# exit
(config-message-spool)# exit
(config)#
```

#### Setting up Solace JNDI References

To enable the JMS clients to connect and look up the Queue destination required by Flink, there are two JNDI objects required on the Solace appliance:

* A connection factory: JNDI/CF/flink
* A queue destination: JNDI/Q/receive

They are configured as follows:

```
(config)# jndi message-vpn Solace_Flink_VPN
(config-jndi)# create connection-factory JNDI/CF/flink
(config-jndi-connection-factory)# property-list messaging-properties
(config-jndi-connection-factory-pl)# property default-delivery-mode persistent
(config-jndi-connection-factory-pl)# exit
(config-jndi-connection-factory)# property-list transport-properties
(config-jndi-connection-factory-pl)# property direct-transport false
(config-jndi-connection-factory-pl)# property "reconnect-retry-wait" "3000"
(config-jndi-connection-factory-pl)# property "reconnect-retries" "20"
(config-jndi-connection-factory-pl)# property "connect-retries-per-host" "5"
(config-jndi-connection-factory-pl)# property "connect-retries" "1"
(config-jndi-connection-factory-pl)# exit
(config-jndi-connection-factory)# exit
(config-jndi)#
(config-jndi)# create queue JNDI/Q/receive
(config-jndi-queue)# property physical-name Q/receive
(config-jndi-queue)# exit
(config-jndi)# 
(config-jndi)# no shutdown
(config-jndi)# exit
(config)#
```

### Step 3 – Coding a JMS SourceFunction for Flink

From [Flink Streaming Documentation](https://ci.apache.org/projects/flink/flink-docs-release-1.2/dev/datastream_api.html){:target="_blank"} there are details on how to build a custom SourceFunction and a template.  In this section of the document will use this template and build a `JMSQueueSource`.

The `JMSQueueSource` extends `org.apache.flink.streaming.api.functions.source.SourceFunction`.  This will result in the following methods created:

* `JMSQueueSource` constructor – Synchronously called once as the `QueueSource` is initially created.
* `void run()` – Asynchronously called once as the SourceFunction is started, which loops accepting messages.  
* `void cancel()` – Asynchronously called once as the Receiver is stopped

The custom `SourceFunction` also implements `ResultTypeQueryable<OUT>;` this helps other generic Flink internals operate with the generic `OUT` type that each specific SourceFunction implementation produces.

* `TypeInformation<OUT> getProducedType()` – invoked by Flink collections to retrieve the `TypeInformation<OUT>` of the custom SourceFunctions generic outputs

```java
public class JMSQueueSource<OUT> implements SourceFunction<OUT>, ResultTypeQueryable<OUT> {
  public JMSQueueSource ( ) {
    }
    @Override
    public void run() {
          // TODO Auto-generated 
    }
    @Override
    public void cancel() {
      // TODO Auto-generated 
    }
  @Override
  public TypeInformation<OUT> getProducedType() {
      // TODO Auto-generated 
  }
}
```

In the constructor we need to collect information to information needed to connect to Solace and build the JMS environment. To make this completely generic to any JMS implementation, this is passed into the constructor in a completely generic way, compliant with JMS 1.1 and JNDI standards. The vendor-agnostic way to do this in JMS is to use a JNDI naming InitialContext to lookup a JMS ConnectionFactory and all JMS resources used (e.g., queues or topics). The JMS ConnectionFactory is used to create a JMS Connection which is then used to consume JMSMessages from JMS Topics or Queues (types for both are provided in the sample connector library).

* `jmsEnvironment` – The JMS environment properties for the InitialContext; this is used to lookup the connection-factory 
* `cfName` – The connection-factory name to retrieve from the JMS InitialContext and use for all connection creation
* `topicName` – Topic string to subscribe to; platform-dependent, will support whatever wildcarding the underlying platform supports
* `deserializer` – A JMS translator instance that knows how to translate specific JMS messages to specific target types

```java
private Hashtable<String, String> _jmsEnv;
private JMSTranslator<OUT> _deserializer;
private String _cfName;
private String _topicName;
public JMSTopicSource(Hashtable<String,String> jmsEnvironment,
                          String cfName, String topicName,
                          JMSTranslator<OUT> deserializer) {
  _jmsEnv = jmsEnvironment;
  _cfName = cfName;
  _topicName = topicName;
  _deserializer = deserializer;
}
```

* Next in the run() method we look up the JMS connection factory and queue then connect to receive messages and store them into Flink.

```java
@Override
public void run() {
  InitialContext initialContext = null;
  try {
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
  } catch (NamingException e) {
    e.printStackTrace();
  } catch (JMSException e) {
    e.printStackTrace();
  }
}
```

### Step 4 – Coding a JMSTranslator for Your SourceFunction

In the example above, we are using a simple `JMSTranslator` function for JMS `TextMessages` that is part of the sample project in GitHub: [src/flink-jms-connector]({{ site.repository }}/blob/master/src/flink-jms-connector){:target="_blank"}. Typically, you would implement a custom translator to marshal the contents of the inbound JMS Messages to the type your Flink application expects. While Flink has internal Serializer implementations, we tactically chose to implement serialization separately as it allows us to account for JMS Message properties when translating them into Flink objects.

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

Note that the JMSTranslator exposes an outputType() method that returns the Class of output for this translator; this function is exposed by both the `JMSTopicSource` and `JMSQueueSource` implementations via the generic ResultTypeQueryable interface implemented for Flink’s generic collections to use:

```java
    @Override
    public TypeInformation<OUT> getProducedType() {
        return TypeInformation.of(_deserializer.outputType());
    }
```

## Working with Solace High Availability (HA)

The [Solace JMS API Online Reference Documentation]({{ site.links-docs-jms-api }}){:target="_top"} section “Establishing Connection and Creating Sessions” provides details on how to enable the Solace JMS connection to automatically reconnect to the standby appliance in the case of a HA failover of a Solace appliance. By default Solace JMS connections will reconnect to the standby appliance in the case of an HA failover.

In general the Solace documentation contains the following note regarding reconnection:

```
Note: When using HA redundant appliances, a fail-over from one appliance to its mate will typically
occur in under 30 seconds, however, applications should attempt to reconnect for at least five minutes. 
```

In the previous section "Setting up Solace JNDI References", the Solace CLI commands correctly configured the required JNDI properties to reasonable values. These commands are repeated here for completeness.

```
(config)# jndi message-vpn Solace_Flink_VPN
(config-jndi)# connection-factory JNDI/CF/flink
(config-jndi-connection-factory)# property-list transport-properties
(config-jndi-connection-factory-pl)# property "reconnect-retry-wait" "3000"
(config-jndi-connection-factory-pl)# property "reconnect-retries" "20"
(config-jndi-connection-factory-pl)# property "connect-retries-per-host" "5"
(config-jndi-connection-factory-pl)# property "connect-retries" "1"
(config-jndi-connection-factory-pl)# exit
(config-jndi-connection-factory)# exit
(config-jndi)# exit
(config)#
```

Finally ensure that the JNDI Destination you are using points to a Topic not a Queue:

```
(config)# jndi message-vpn Solace_Flink_VPN
(config-jndi)# create topic JNDI/T/recieve
(config-jndi-queue)# property physical-name Topic/Recieve
(config-jndi-queue)# exit
```

## Debugging Tips for Solace JMS API Integration

The key component for debugging integration issues with the Solace JMS API is the API logging that can be enabled. How to enable logging in the Solace API is described below.

### How to enable Solace JMS API logging

Spark was written using Jakarta Commons Logging API (JCL), Solace JMS API also makes use of the Jakarta Commons Logging API (JCL), configuring the Solace JMS API logging is very similar to configuring any other Spark application. The following example shows how to enable debug logging in the Solace JMS API using log4j.

One note to consider is that since the Solace JMS API has a dependency on the Solace Java API (JCSMP) both of the following logging components should be enabled and tuned when debugging to get full information. For example to set both to debug level:

```
log4j.category.com.solacesystems.jms=DEBUG
log4j.category.com.solacesystems.jcsmp=DEBUG
```

By default info logs will be written to the consol. This section will focus on using log4j as the logging library and tuning Solace JMS API logs using the log4j properties. Therefore in order to enable Solace JMS API logging, a user must do two things:

* Put Log4j on the classpath.
* Create a log4j.properties configuration file in the root folder of the classpath

Below is an example Log4j properties file that will enable debug logging within the Solace JMS API.

```
log4j.rootCategory=INFO, stdout
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%d{ABSOLUTE} %5p %t %c{2}:%L - %m%n
log4j.category.com.solacesystems.jms=DEBUG
log4j.category.com.solacesystems.jcsmp=DEBUG
```

With this you can get output in a format similar to the following which can help in understanding what is happening within the Solace JMS API.

```
14:35:01,171 DEBUG main client.ClientRequestResponse:75 - Starting request timer (SMP-EstablishP2pSub) (10000 ms)
14:35:01,171 DEBUG Context_2_ReactorThread client.ClientRequestResponse:83 - Stopping request timer (SMP-EstablishP2pSub)
14:35:01,173  INFO main jms.SolConnection:151 - Connection created.
14:35:01,173  INFO main connection.CachingConnectionFactory:298 - Established shared JMS Connection: com.solacesystems.jms.SolConnection@ca3f2d
14:35:01,180  INFO main jms.SolConnection:327 - Entering start()
14:35:01,180  INFO main jms.SolConnection:338 - Leaving start() : Connection started.
14:35:01,180  INFO jmsContainer-1 jms.SolConnection:252 - Entering createSession()
```

## Sample Code Reference

A working sample with maven pom build is provided in GitHub with this guide:

*    [src/flink-jms-connector]({{ site.repository }}/blob/master/src/flink-jms-connector){:target="_blank"}


