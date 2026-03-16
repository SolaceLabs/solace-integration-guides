---
layout: guides
title: Spark Streaming 3.2
summary: This document is an integration guide for using Solace PubSub+ as a JMS provider for an Apache Spark Streaming custom receiver.Apache Spark is a fast and general-purpose cluster computing system. It provides an optimized engine that supports general execution graphs. It also supports a rich set of higher-level tools including Spark SQL for SQL and structured data processing, MLib for machine learning, GraphX for graph processing, and Spark Streaming for high-throughput, fault-tolerant stream processing of live data streams. The Spark Streaming custom receiver is a simple interface that allows third party applications to push data into Spark in an efficient manner.
icon: spark-streaming.png
links:
   - label: Example Source Code - Spark Streaming
     link: https://github.com/SolaceLabs/solace-integration-guides/blob/master/src/spark-streaming
---

## Overview

This document demonstrates how to integrate Solace Java Message Service (JMS) with the Spark Streaming custom receiver for consumption of JMS messages. The goal of this document is to outline best practices for this integration to enable efficient use of both the Spark Streaming and Solace JMS.

The target audience of this document is developers using the Hadoopv2 with knowledge of both the Spark and JMS in general. As such this document focuses on the technical steps required to achieve the integration. For detailed background on either Solace JMS or Spark refer to the referenced documents below.

This document is divided into the following sections to cover the Solace JMS integration with Spark Streaming:
* [Integrating with Spark Streaming](#integrating-with-spark-streaming)
* [Performance Considerations](#performance-considerations)
* [Working with Solace High Availability](#working-with-solace-high-availability-ha)
* [Debugging Tips](#debugging-tips-for-solace-jms-api-integration)
* Advanced Topics including:
  * [Authentication (LDAP example)](#authentication-ldap-example)
  * [Using TLS Communication](#using-tls-communication)
  * [Working with Solace Disaster Recovery](#working-with-solace-disaster-recovery)

### Related Documentation

These links contain information related to this guide:

* [Solace Developer Portal]({{ site.links-dev-portal }}){:target="_top"}
* [Solace Messaging API for JMS]({{ site.links-docs-jms }}){:target="_top"}
* [Solace JMS API Online Reference Documentation]({{ site.links-docs-jms-api }}){:target="_top"}
* [Solace Feature Guide]({{ site.links-docs-features }}){:target="_top"}
* [Solace PubSub+ Event Broker Configuration]({{ site.links-docs-router-config }}){:target="_top"}
* [Solace Command Line Interface Reference]({{ site.links-docs-cli }}){:target="_top"}
* [Spark Streaming Custom Receivers Documentation](https://spark.apache.org/docs/latest/streaming-custom-receivers.html)
* [Spark Receiver Class Documentation](https://spark.apache.org/docs/latest/api/java/org/apache/spark/streaming/receiver/Receiver.html)

{% include_relative assets/solaceMessaging.md %}

## Integrating with Spark Streaming<a name="integrating-with-spark-streaming"></a>

The general Spark Streaming support for custom receivers is documented in the [Spark Streaming Custom Receivers Documentation](https://spark.apache.org/docs/latest/streaming-custom-receivers.html). The configuration outlined in this document makes use of a custom receiver to achieve the desired integration with Solace.

This integration guide demonstrates how to configure a Spark Streaming application to receive JMS messages using a custom receiver. The following steps are required to accomplish this:
* Step 1 – Configuring the Solace PubSub+ event broker
* Step 2 – Coding a JMS custom receiver
* Step 3 – Deploying JMS receiver

### Description of Resources Required

This integration guide will demonstrate creation of Solace JMS custom receiver and configuring the receiver to receive messages. This section outlines the resources that are required/created and used in the subsequent sections.

#### Solace Resources

The following event broker resources are required for the integration sample in this document.

<table>
    <tr>
      <th>Resource</th>
      <th>Value</th>
      <th>Description</th>
    </tr>
    <tr>
      <td>Solace Event Broker Host</td>
      <td colspan="2" rowspan="4">Refer to section <a href="#get-solace-messaging">Get Solace Messaging</a>  for values</td>
    </tr>
    <tr>
      <td>Message VPN</td>
    </tr>
    <tr>
      <td>Client Username</td>
    </tr>
    <tr>
      <td>Client Password</td>
    </tr>
    <tr>
      <td>PubSub+ Queue</td>
      <td>Q/receiver</td>
      <td>Solace destination of persistent messages consumed</td>
    </tr>
    <tr>
      <td>JNDI Connection Factory</td>
      <td>JNDI/Sol/CF</td>
      <td>The JNDI Connection factory for controlling Solace JMS connection properties</td>
    </tr>
    <tr>
      <td>JNDI Queue Name</td>
      <td>JNDI/Q/receive</td>
      <td>The JNDI name of the queue used in the samples</td>
    </tr>
</table>

#### Spark Resources

The following Spark resources are required for code integration:

| **Resource** | **Description** |
| org.apache.spark.storage.StorageLevel | |
| org.apache.spark.streaming.receiver.Receiver | Class implementing Receiver |

### Step 1 – Configuring the Solace Event Broker

The Solace event broker needs to be configured with the following configuration objects at a minimum to enable JMS to send and receive messages within the Spark application.

* A Message VPN, or virtual event broker, to scope the integration on the Solace event broker.
* Client connectivity configurations like usernames and profiles
* Guaranteed messaging endpoints for receiving and sending messages.
* Appropriate JNDI mappings enabling JMS clients to connect to the Solace event broker configuration.

{% include_relative assets/solaceConfig.md %}

{% include_relative assets/solaceVpn.md content="solace_VPN" %}

#### Configuring Client Usernames & Profiles

This section outlines how to update the default client-profile and how to create a client username for connecting to the Solace event broker. For the client-profile, it is important to enable guaranteed messaging for JMS messaging and transacted sessions if using transactions.

The chosen client username of “spark_user” will be required by the Spark application when connecting to the Solace event broker.

```
(config)# client-profile default message-vpn Solace_Spark_VPN
(config-client-profile)# message-spool allow-guaranteed-message-receive
(config-client-profile)# message-spool allow-guaranteed-message-send
(config-client-profile)# message-spool allow-transacted-sessions
(config-client-profile)# exit
(config)#
(config)# create client-username spark_user message-vpn Solace_Spark_VPN
(config-client-username)# acl-profile default 
(config-client-username)# client-profile default
(config-client-username)# no shutdown
(config-client-username)# exit
(config)#
```

#### Setting up Guaranteed Messaging Endpoints

This integration guide shows receiving messages within the Spark application from a single JMS Queue. For illustration purposes, this queue is chosen to be an exclusive PubSub+ queue with a message spool quota of 2GB matching quota associated with the message VPN. The queue name chosen is “Q/requests”.

```
(config)# message-spool message-vpn Solace_Spark_VPN
(config-message-spool)# create queue Q/receive
(config-message-spool-queue)# access-type exclusive
(config-message-spool-queue)# max-spool-usage 2000
(config-message-spool-queue)# permission all delete
(config-message-spool-queue)# no shutdown
(config-message-spool-queue)# exit
(config-message-spool)# exit
(config)#
```

#### Setting up Solace JNDI References<a name="setting-up-solace-jndi-references"></a>

To enable the JMS clients to connect and look up the Queue destination required by Spark, there are two JNDI objects required on the Solace  event broker:
* A connection factory: JNDI/Sol/CF
  * Note: Ensure `direct-transport` is disabled for JMS persistent messaging
* A queue destination: JNDI/Q/receive

They are configured as follows:

```
(config)# jndi message-vpn Solace_Spark_VPN
(config-jndi)# create connection-factory JNDI/Sol/CF
(config-jndi-connection-factory)# property-list messaging-properties
(config-jndi-connection-factory-pl)# property default-delivery-mode persistent
(config-jndi-connection-factory-pl)# exit
(config-jndi-connection-factory)# property-list transport-properties
(config-jndi-connection-factory-pl)# property direct-transport false
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

### Step 2 – Coding a JMS custom receiver

From [Spark Receiver Class Documentation](https://spark.apache.org/docs/latest/api/java/org/apache/spark/streaming/receiver/Receiver.html) there is details on how to build a custom receiver and a template. We will use this template and build a `PubSubPlusJMSReceiver`, which will stream events from the PubSub+ event broker through a JMS connection, using JNDI lookup of the queue to receive from.

The `PubSubPlusJMSReceiver` extends the `org.apache.spark.streaming.receiver.Receiver` and implements the `javax.jms.MessageListener`.  This will result in the following methods created:
* `PubSubPlusJMSReceiver` constructor – Synchronously called once as the Receiver is initially created.
* `org.apache.spark.streaming.receiver.Receiver.onStart()` – Asynchronously called once as the Receiver is started.  
* `org.apache.spark.streaming.receiver.Receiver.onStop()` – Asynchronously called once as the Receiver is stopped
* `javax.jms.MessageListener.onMessage()` – Asynchronously called on every message received from Solace

```Java
public class PubSubPlusJMSReceiver extends Receiver<String> implements MessageListener {
  private static final long serialVersionUID = 1L;
  private static final String SOLJMS_INITIAL_CONTEXT_FACTORY =
              "com.solacesystems.jndi.SolJNDIInitialContextFactory";

  public JMSReceiver( ) {
    super(StorageLevel.MEMORY_ONLY_SER_2());
  }

  @Override
  public void onStart() {
    // TODO Auto-generated from spark.streaming.receiver
  }

  @Override
  public void onStop() {
    // TODO Auto-generated from spark.streaming.receiver
  }

  @Override
  public void onMessage(Message arg0) {
    // TODO Auto-generated from javax.jms.MessageListener 
  }
}
```

In the constructor we need to collect information to information needed to connect to Solace and build the JMS environment.

```Java
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
```

Next, in the `onStart()` method we need to look up the JMS connection factory and queue then connect to receive messages:

```Java
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
    ConnectionFactory factory = (ConnectionFactory) context.lookup(_connectionFactory);
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
```

Finally, when receiving messages from the PubSub+ broker they need to be stored into Spark:

```Java
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
 ```

### Step 3 – Deploying JMS Receiver

This section will demo the use of the JMS receiver by creating a Java Spark Streaming example program, which counts the words in the input stream from the JMS receiver. For details of how the example works refer to the Java version of the [Spark Streaming Quick Start documentation](https://spark.apache.org/docs/latest/streaming-programming-guide.html#a-quick-example), which demonstrates a similar use case.

Following code snippet shows how `PubSubPlusJMSReceiver` is invoked:

```Java
public static void main(String[] args) throws Exception {
  // Create the context with a 1 second batch size
  SparkConf sparkConf = new SparkConf().setAppName("JavaCustomReceiver");
  JavaStreamingContext ssc = new JavaStreamingContext(sparkConf, new Duration(1000));
  // Create a input stream with the custom receiver on provided PubSub+ broker connection config
  JavaReceiverInputDStream<String> lines = ssc.receiverStream(
      new PubSubPlusJMSReceiver(args[0], args[1], args[2], args[3], args[4], args[5],
                                StorageLevel.MEMORY_ONLY_SER_2()));
  ...
}
```

#### Demo pre-requisites

* Working Spark deployment: this can be a cluster of Spark machines or a standalone local installation.
* Working and configured Solace PubSub+ deployment - refer to the previous sections.

#### Demo run

The demo code shall be first built, then submitted to Spark. Within a cluster of Spark machines you can build and submit jobs from any machine. For simplicity, a local Spark installation is assumed here.

1. Clone this project from GitHub
```bash
git clone https://github.com/SolaceLabs/solace-integration-guides.git
cd solace-integration-guides/src/spark-streaming
export PROJECT_HOME=$(pwd)
```
1. Build the demo
```bash
mvn package
# this will generate jar libraries
# note the jar variant that includes required dependencies
ls target/*.jar
```
1. Submit to Spark
```bash
# Substitute "/path/to/spark" to local Spark install directory
export SPARK_HOME="/path/to/spark"
# Substitute from the configuration described in the Solace Resources section
export HOST=" <Solace Event Broker Host>"
export MESSAGE_VPN="<Message VPN>"
export USERNAME="<Client Username>"
export PASSWORD="<Client Password>"
export JNDI_QUEUE="<JNDI Queue Name>"
export JNDI_CF="<JNDI Connection Factory>"
# This example configures 4 local threads in the --master parameter
$SPARK_HOME/bin/spark-submit \
  --class com.solace.sample.PubSubPlusJMSReceiverTest \
  --master local[4] \
  $PROJECT_HOME/target/pubsubplus-jms-to-spark-streaming-demo-0.0.1-jar-with-dependencies.jar \
  $HOST \
  $MESSAGE_VPN \
  $USERNAME \
  $PASSWORD \
  $JNDI_QUEUE \
  $JNDI_CF
```
1. Send test messages to the PubSub+ queue, which will be streamed to Spark, then observe the logs. You can send individual messages from the [Try Me!](https://docs.solace.com/Broker-Manager/PubSub-Manager-Overview.htm?Highlight=Try%20Me#Test-Messages) tab of the PubSub+ Broker Manager. To generate larger number of messages it is recommended to use the [SDKPerf tool](https://docs.solace.com/SDKPerf/SDKPerf.htm).

## Performance Considerations<a name="performance-considerations"></a>

In the provided example above persistent messaging was used on the  event broker and the Spark Streaming client connected to a queue. This design pattern provides the highest level of reliability as each message is persisted on the Solace Event Broker and will not be lost in case of a client failure.  This message pattern consumes the most resources on the Solace Event Broker and is not the most performant.

If the client does not want to receive messages that where missed while it was offline, does not want to receive older messages if it is unable to keep up to the published message flow, or wants the highest throughput with lowest latency; then direct messaging is the recommended pattern.

To achieve direct messaging, configure the connection-factory to enable this feature.
```
(config)# jndi message-vpn Solace_Spark_VPN
(config-jndi)# connection-factory JNDI/Sol/CF
(config-jndi-connection-factory)# property-list transport-properties
(config-jndi-connection-factory-pl)# property direct-transport true

(config)# jndi message-vpn Solace_Spark_VPN
(config-jndi)# create topic JNDI/T/receive
(config-jndi-queue)# property physical-name T/receive
(config-jndi-queue)# exit
```

## Working with Solace High Availability (HA)<a name="working-with-solace-high-availability-ha"></a>

The [Solace Messaging API for JMS]({{ site.links-docs-jms }}){:target="_top"} section "Establishing Connection and Creating Sessions" provides details on how to enable the Solace JMS connection to automatically reconnect to the standby event broker in the case of a HA failover of a event broker. By default Solace JMS connections will reconnect to the standby event broker in the case of an HA failover.

In general the Solace documentation contains the following note regarding reconnection:

Note: When using HA redundant event brokers, a fail-over from one event broker to its mate will typically occur in less than 30 seconds, however, applications should attempt to reconnect for at least five minutes. 

In "Setting up Solace JNDI References", the Solace CLI commands correctly configured the required JNDI properties to reasonable values. Note: the retry parameters re all defaults, these commands are repeated here for completeness. 

```
(config)# jndi message-vpn solace_VPN
(config-jndi)# create connection-factory JNDI/Sol/CF
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

## Debugging Tips for Solace JMS API Integration<a name="debugging-tips-for-solace-jms-api-integration"></a>

The key component for debugging integration issues with the Solace JMS API is to enable API logging.
Spark was written using Jakarta Commons Logging API (JCL), Solace JMS API also makes use of the Jakarta Commons Logging API (JCL), configuring the Solace JMS API logging is very similar to configuring any other Spark application. The following example shows how to enable debug logging in the Solace JMS API using log4j.
One note to consider is that since the Solace JMS API has a dependency on the Solace Java API (JCSMP) both of the following logging components should be enabled and tuned when debugging to get full information. By default info logs will be written to the consol. Following example sets both to debug level:
```
log4j.category.com.solacesystems.jms=DEBUG
log4j.category.com.solacesystems.jcsmp=DEBUG
```
Above lines can be added to the `log4j.properties` configuration file located at the `$SPARK_HOME/conf` directory. Note: if no `log4j.properties` configuration file exists then the contents of the `log4j.properties.template` file can be used as a starting point.
With this you can get output in a format similar to the following which can help in understanding what is happening within the Solace JMS API.

```log
22/01/31 09:11:22 INFO TcpClientChannel: Client-2: Connecting to host 'orig=tcps://mr-xyz.messaging.solace.cloud:55443, scheme=tcps://, host=mr-xyz.messaging.solace.cloud, port=55443' (host 1 of 1, smfclient 2, attempt 1 of 1, this_host_attempt: 1 of 1)
22/01/31 09:11:22 INFO SNIUtil: Server Name Indication (SNI) automatically applied by using provided hostname
22/01/31 09:11:22 INFO SSLSmfClient: SSLEngine Supported Protocols: [SSLV3, TLSV1, TLSV1.1, TLSV1.2]
22/01/31 09:11:22 INFO SSLSmfClient: Application Specified Protocols: [SSLv3, TLSv1, TLSv1.1, TLSv1.2]
22/01/31 09:11:22 INFO TcpClientChannel: Client-2: Connected to host 'orig=tcps://mr-xyz.messaging.solace.cloud:55443, scheme=tcps://, host=mr-xyz.messaging.solace.cloud, port=55443' (smfclient 2)
```

## Advanced Topics

### Authentication (LDAP example)<a name="authentication-ldap-example"></a>

JMS Client authentication is handled by the PubSub+ event broker. The broker supports a variety of authentications schemes as described in [the Solace documentation](https://docs.solace.com/Configuring-and-Managing/Configuring-Client-Authentication.htm).
In this section we will show how to configure the PubSub+ Event Broker to pass the authentication username/password through to an LDAP,(Active-Directory) server to incorporate with enterprise level authentication mechanisms.  TLS client certificate, OAuth and Kerberos are also possible.
* First an LDAP profile needs to be created, this includes:
* Admin Username and Password to do LDAP lookups
* Part of the LDAP structure to check for users
* Location of LDAP server(s)
* Search filter, how to compare Client Username to LDAP Structure.

```
(config)# create authentication ldap-profile ActiveDirectoryIntegration
(config/authentication/ldap-profile)# admin dn DomainAdmin password xxxxxx
(config/authentication/ldap-profile)# search base-dn dc=lab,dc=solace,dc=com
(config/authentication/ldap-profile)# ldap-server ldap://192.168.1.56 index 1
(config/authentication/ldap-profile)# search filter "(sAMAccountName = $CLIENT_USERNAME)"
(config/authentication/ldap-profile)# no shut
(config/authentication/ldap-profile)# exit
```
Finally the LDAP profile will need to be enabled for the message VPN.  Note that there is no code change from the Application/API.  As the authentication is pass-through from the  event broker to the LDAP server.
```
(config)# message-vpn Solace_Spark_VPN
(config/message-vpn)# authentication user-class client
(...message-vpn/authentication/user-class)# basic
(...e-vpn/authentication/user-class/basic)# auth-type ldap ActiveDirectoryIntegration
(...e-vpn/authentication/user-class/basic)# exit
```
### Enabling TLS on the PubSub+ Event Broker<a name="using-tls-communication"></a>

TLS may already be enabled on your Event Broker, for example if using PubSub+ Cloud. In this case you can skip this section.

To enable secure connections (TLS) to the Solace event broker, the following configuration must be updated on the Solace event broker.

* Server Certificate
* TLS/SSL Service Listen Port
* Enable TLS/SSL over SMF in the Message VPN

The following sections outline how to configure these items.

##### Configuring the Server Certificate

Before starting, here is some background information on the server certificate required by the event broker. This is from the [Solace documentation](https://docs.solace.com/Configuring-and-Managing/Managing-Server-Certs.htm ):

"To enable TLS/SSL-encryption, you must set the TLS/SSL server certificate file that the Solace PubSub+ event broker is to use. This server certificate is presented to clients during TLS/SSL handshakes. The server certificate must be an x509v3 certificate and include a private key. The server certificate and key use an RSA algorithm for private key generation, encryption and decryption, and they both must be encoded with a Privacy Enhanced Mail (PEM) format."

To configure the server certificate, first copy the server certificate to the Solace event broker. For the purposes of this example, assume the server certificate file is named "mycert.pem".

```
# copy sftp://[<username>@]<ip-addr>/<remote-pathname>/mycert.pem /certs
<username>@<ip-addr>'s password:
#
```

Then set the server certificate for the Solace event broker.

```
(config)# ssl server-certificate mycert.pem
(config)#
```

##### Configure TLS/SSL Service Listen Port

By default, the Solace event broker accepts secure messaging client connections on port 55443. If this port is acceptable then no further configuration is required and this section can be skipped. If a non-default port is desired, then follow the steps below. Note this configuration change will disrupt service to all clients of the Solace event broker and should therefore be performed during a maintenance window when this client disconnection is acceptable. This example assumes that the new port should be 55403.

```
(config)# service smf
(config-service-smf)# shutdown
All SMF and WEB clients will be disconnected.
Do you want to continue (y/n)? y
(config-service-smf)# listen-port 55403 ssl
(config-service-smf)# no shutdown
(config-service-smf)# exit
(config)#
```

##### Enable TLS/SSL within the Message VPN 

By default within Solace message VPNs both the plain-text and SSL services are enabled. If the Message VPN defaults remain unchanged, then this section can be skipped. However, if within the current application VPN, this service has been disabled, then for secure communication to succeed it should be enabled. The steps below show how to enable SSL within the messaging (SMF) service to allow secure client connections from the Spark Streaming client. 

```
(config)# message-vpn solace_VPN
(config-msg-vpn)# service smf
(config-msg-vpn-service-smf)# ssl
(config-msg-vpn-service-ssl)# no shutdown
(config-msg-vpn-service-ssl)# exit
(config-msg-vpn-service-smf)# exit
(config-msg-vpn-service)# exit
(config-msg-vpn)# exit
(config)#
```

### Configuring Secure Connections on the Spark Client

##### Updating the provider URL

In order to signal to the PubSub+ JMS API that the connection should be a secure connection, the protocol must be updated in "Solace Event Broker Host":
```
tcps://<host>:<port>
```

##### Adding SSL Related Configuration

Additionally, if using a self-signed server certificate, the Solace JMS API must be able to validate the server certificate of the Solace Event Broker in order to establish a secure connection. To do this, the following trust store parameters need to be provided in the jndi.properties file:

First the Solace JMS API must be given a location of a trust store file so that it can verify the credentials of the Solace Event Broker server certificate during connection establishment. This parameter takes a URL or Path to the trust store file.
```
env.put(SupportedProperty.Solace_JMS_SSL_TrustStore, ___Path_or_URL___)
```

It is also required to provide a trust store password. This password allows the Solace JMS API to validate the integrity of the contents of the trust store. This is done through the following parameter.
```
env.put(SupportedProperty.Solace_JMS_SSL_TrustStorePassword, ___Password___)
```

There are multiple formats for the trust store file. By default Solace JMS assumes a format of Java Key Store (JKS). So if the trust store file follows the JKS format then this parameter may be omitted. Solace JMS supports two formats for the trust store: “jks” for Java Key Store or “pkcs12”. Setting the trust store format is done through the following parameter.
```
env.put(SupportedProperty.Solace_JMS_SSL_TrustStoreFormat, jks)
```

And finally, the authentication scheme must be selected. Solace JMS supports the following authentication schemes for secure connections:
* AUTHENTICATION_SCHEME_BASIC 
* AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE

This integration example will use basic authentication. So the required parameter is as follows:
```
env.put(SupportedProperty.Solace_JMS_Authentication_Scheme,AUTHENTICATION_SCHEME_BASIC)
```

### Working with Solace Disaster Recovery<a name="working-with-solace-disaster-recovery"></a>

The [Solace-FG] section "Data Center Replication" contains a sub-section on "Application Implementation" which details items that need to be considered when working with Solace"s Data Center Replication feature. This integration guide will show how the following items required to have a Spark Streaming client successfully connect to a backup data center using the Solace Data Center Replication feature.

* Configuring a Host List within the Spark Streaming client
* Configuring JMS Reconnection Properties within Solace JNDI
* Configuring Message Driven Bean Re-activation in the Event of Activation Failures
* Disaster Recovery Behavior Notes

#### Configuring a Host List within the Spark Streaming client

As described in [Solace-Docs], the host list provides the address of the backup data center. This is configured within the Spark Streaming client through the ConnectionURL configuration property value (of a respective JCA entity) as follows:

```
tcp://__IP_active_site:PORT__,tcp://__IP_standby_site:PORT__
```

The active site and standby site addresses are provided as a comma-separated list of "Connection URIs".  When connecting, the Solace JMS connection will first try the active site and if it is unable to successfully connect to the active site, then it will try the standby site. This is discussed in much more detail in the referenced Solace documentation

#### Configuring reasonable JMS Reconnection Properties within Solace JNDI

In order to enable applications to successfully reconnect to the standby site in the event of a data center failure, it is required that the Solace JMS connection be configured to attempt connection reconnection for a sufficiently long time to enable the manual switch-over to occur. This time is application specific depending on individual disaster recovery procedures and can range from minutes to hours depending on the application. In general it is best to tune the reconnection by changing the "reconnect retries" parameter within the Solace JNDI to a value large enough to cover the maximum time to detect and execute a disaster recovery switch over. If this time is unknown, it is also possible to use a value of "-1" to force the Solace JMS API to reconnect indefinitely.

The reconnect retries is tuned in the Solace event broker CLI as follows:

```
config)# jndi message-vpn solace_VPN
(config-jndi)# connection-factory JNDI/Sol/CF
(config-jndi-connection-factory)# property-list transport-properties
(config-jndi-connection-factory-pl)# property "reconnect-retries" "-1"
(config-jndi-connection-factory-pl)# exit
(config-jndi-connection-factory)# exit
(config-jndi)# exit
(config)#
```
