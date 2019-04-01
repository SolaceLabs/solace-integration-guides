---
layout: guides
title: Red Hat JBoss EAP v6 and v7
summary: The JBoss Enterprise Application Server provides a comprehensive framework for application and integration middleware that is compliant with the Java Enterprise Edition computing platform. Solace provides a Java Connector Architecture (JCA) compliant Resource Adapter that may be deployed to the JBoss Application Server providing enterprise applications with connectivity to the Solace message broker.
icon: red-hat-jboss-eap.png
links:
   - label: Example Source Code - JBoss EAP
     link: https://github.com/SolaceLabs/solace-integration-guides/blob/master/src/jboss-eap
---

## Overview

This document demonstrates how to integrate Solace Java Message Service (JMS) with the JBoss Application Server EAP v6 and v7 for production and consumption of JMS messages. The goal of this document is to outline best practices for this integration to enable efficient use of both the application server and Solace JMS. 

The target audience of this document is developers using the JBoss Application Server with knowledge of both the JBoss Application Server and JMS in general. As such this document focuses on the technical steps required to achieve the integration. 

Note: this document provides instructions on configuring and deploying the Solace JCA 1.5 resource adapter in JBoss EAP v6 and v7 (Enterprise Application Platform).  For detailed background on either Solace JMS or the JBoss Application Server refer to the referenced documents below.

This document is divided into the following sections to cover the Solace JMS integration with JBoss Application Server:

* [Integrating with JBoss Application Server](#integrating-with-jboss-application-server)
* [Performance Considerations](#performance-considerations)
* [Working with Solace High Availability](#working-with-solace-high-availability-ha)
* [Debugging Tips ](#debugging-tips-for-solace-jms-api-integration)
* Advanced Topics including:
  * [Using SSL Communication](#using-ssl-communication)
  * [Working with Transactions](#working-with-transactions)
  * [Working with Solace Disaster Recovery](#working-with-solace-disaster-recovery)
  * [Using an external JNDI store for Solace JNDI lookups](#using-an-external-jndi-store-for-solace-jndi-lookups)


### Related Documentation

These links contain information related to this guide:

* [Solace Developer Portal]({{ site.links-dev-portal }}){:target="_top"}
* [Solace Messaging API for JMS]({{ site.links-docs-jms }}){:target="_top"}
* [Solace JMS API Online Reference Documentation]({{ site.links-docs-jms-api }}){:target="_top"}
* [Solace Feature Guide]({{ site.links-docs-features }}){:target="_top"}
* [Solace PubSub+ Message Broker Configuration]({{ site.links-docs-router-config }}){:target="_top"}
* [Solace Command Line Interface Reference]({{ site.links-docs-cli }}){:target="_top"}
* [JBoss Enterprise Application Platform Documentation](https://access.redhat.com/documentation/en/red-hat-jboss-enterprise-application-platform/){:target="_blank"}
* [JBoss Enterprise Application Platform 7.0 Security Guide](https://access.redhat.com/documentation/en/red-hat-jboss-enterprise-application-platform/7.0/how-to-configure-server-security/how-to-configure-server-security){:target="_blank"}
* [Java Connector Architecture v1.5](https://jcp.org/en/jsr/detail?id=112 ){:target="_blank"}

{% include_relative assets/solaceMessaging.md %}

## Integrating with JBoss Application Server<a name="integrating-with-jboss-application-server"></a>

Solace provides a JCA compliant resource adapter for integrating Java enterprise applications with the Solace PubSub+ message broker.  There are several options for deploying a Resource Adapter for use by Java enterprise applications including embedded and stand-alone deployment.  Solace provides a Resource Adapter Archive (RAR) file for stand-alone deployment.

In order to illustrate JBoss Application Server integration, the following sections will highlight the required JBoss configuration changes, and provide sample code for sending and receiving messages using Enterprise Java Beans. 

This EJB sample consists of two enterprise beans, a Message Driven Bean and a Session Bean.  The MDB is configured to receive a message on a `requests` Queue.  When the MDB receives a message it then calls a method of the Session Bean to send a reply message to a `replies` Queue.  The EJB sample requires configuration of various J2C entities in JBoss to support usage of the Solace JCA compliant resource adapter.

The following steps are required to accomplish the above goals of sending and receiving messages using the Solace JMS message broker. 

* Step 1 - Configure the Solace PubSub+ message broker
* Step 2 – Deploy the Solace Resource Adapter to the JBoss Application Server
* Step 3 – Connect to Solace JMS provider
  * Configure resource adapter
  * Create and configure JMS connection factory
* Step 4 – Receive inbound messages using Solace JMS provider
  * Create and configure Activation specification
* Step 5 – Send outbound messages using Solace JMS provider
  * Create and configure JMS administered object


### Description of Resources Required

The Solace JCA 1.5 resource adapter is provided as a standalone RAR file and is versioned together with a specific release of the Solace JMS API.  The JMS API libraries are bundled inside a single resource adapter RAR file for deployment to the JBoss Application Server.

| **Resource** | **File Location** |
| Solace JCA 1.5 resource adapter stand-alone RAR file | <a href="http://dev.solace.com/downloads/download_jms-api/" target="_top">http://dev.solace.com/downloads/download_jms-api/</a> or <br/> https://sftp.solacesystems.com/~[customer]/[version]/Topic_Routing/APIs/JMS/Current/[release]/sol-jms-ra-[release].rar |

This integration guide will demonstrate creation of Solace resources and configuration of the JBoss Application Server managed resources. The section below outlines the resources that are created and used in the subsequent sections.

#### Solace Resource Naming Convention

To illustrate this integration example, all named resources created on the Solace appliance will have the following prefixes:

| **Resource** | **Prefix** |
| Non-JNDI resource | solace_%RESOURCE_NAME% |
| JNDI names | JNDI/Sol/%RESOURCE_NAME% |

#### Solace Resources

The following message broker resources are required for the integration sample in this document.

<table>
    <tr>
      <th>Resource</th>
      <th>Value</th>
      <th>Description</th>
    </tr>
    <tr>
      <td>Solace Message Broker Host</td>
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
      <td>Solace Queue</td>
      <td>solace_requests</td>
      <td>Solace destination for messages consumed by JEE enterprise application</td>
    </tr>
    <tr>
      <td>Solace Queue</td>
      <td>solace_replies</td>
      <td>Solace destination for messages produced by JEE enterprise application</td>
    </tr>
    <tr>
      <td>JNDI Connection Factory</td>
      <td>JNDI/Sol/CF</td>
      <td>The JNDI Connection factory for controlling Solace JMS connection properties</td>
    </tr>
    <tr>
      <td>JNDI Queue Name</td>
      <td>JNDI/Sol/Q/requests</td>
      <td>The JNDI name of the queue used in the samples</td>
    </tr>
    <tr>
      <td>JNDI Queue Name</td>
      <td>JNDI/Sol/Q/replies</td>
      <td>The JNDI name of the queue used in the samples</td>
    </tr>
</table>

#### Application Server Resource Naming Convention

To illustrate this integration example, all JNDI names local to the JBoss application server have the following prefix:

| **Resource** | **Prefix** |
| JNDI names | java:/jms/[resource name] |

#### Application Server Resources

The following JBoss Application Server resources are required for the integration example in this document.

| **Resource** | **Value** | **Description** |
| Resource Adapter | N/A | The name of the Solace JMS Resource Adapter module as referenced in the JBoss ‘resource-adapters:4.0’ subsystem. |
| JCA connection factory | java:/jms/CF | The connection factory resource referenced by EJB code to perform a JNDI lookup of a Solace javax.jms.ConnectionFactory |
| JCA administered object | java:/jms/Q/requests | The administered object resource referenced by EJB code to perform a JNDI lookup of a Solace javax.jms.Queue |
| JCA administered object | java:/jms/Q/replies | The administered object resource referenced by EJB code to perform a JNDI lookup of a Solace javax.jms.Queue |

### Step 1: Solace JMS provider Configuration

The following entities on the Solace message broker need to be configured at a minimum to enable JMS to send and receive messages within the JBoss Application Server. 

* A Message VPN, or virtual message broker, to scope the integration on the Solace message broker.
* Client connectivity configurations like usernames and profiles
* Guaranteed messaging endpoints for receiving and sending messages.
* Appropriate JNDI mappings enabling JMS clients to connect to the Solace message broker configuration.

{% include_relative assets/solaceConfig.md %}

{% include_relative assets/solaceVpn.md content="solace_VPN" %}

#### Configuring Client Usernames & Profiles

This section outlines how to update the default client-profile and how to create a client username for connecting to the Solace message broker. For the client-profile, it is important to enable guaranteed messaging for JMS messaging and transacted sessions if using transactions.

The chosen client username of "solace_user" will be required by the JBoss Application Server when connecting to the Solace message broker.

```
(config)# client-profile default message-vpn solace_VPN
(config-client-profile)# message-spool allow-guaranteed-message-receive
(config-client-profile)# message-spool allow-guaranteed-message-send
(config-client-profile)# message-spool allow-guaranteed-endpoint-create
(config-client-profile)# message-spool allow-transacted-sessions
(config-client-profile)# exit
(config)#
(config)# create client-username solace_user message-vpn solace_VPN
(config-client-username)# acl-profile default	
(config-client-username)# client-profile default
(config-client-username)# no shutdown
(config-client-username)# exit
(config)#
```

#### Setting up Guaranteed Messaging Endpoints

This integration guide shows receiving messages and sending reply messages within the JBoss Application Server using two separate JMS Queues. For illustration purposes, these queues are chosen to be exclusive queues with a message spool quota of 2GB matching quota associated with the message VPN. The queue names chosen are "solace_requests" and "solace_replies".

```
(config)# message-spool message-vpn solace_VPN
(config-message-spool)# create queue solace_requests
(config-message-spool-queue)# access-type exclusive
(config-message-spool-queue)# max-spool-usage 2000
(config-message-spool-queue)# permission all delete
(config-message-spool-queue)# no shutdown
(config-message-spool-queue)# exit
(config-message-spool)# create queue solace_replies
(config-message-spool-queue)# access-type exclusive
(config-message-spool-queue)# max-spool-usage 2000
(config-message-spool-queue)# permission all delete
(config-message-spool-queue)# no shutdown
(config-message-spool-queue)# exit
(config-message-spool)# exit
(config)#
```

#### Setting up Solace JNDI References<a name="setting-up-solace-jndi-references"></a>

To enable the JMS clients to connect and look up the Queue destination required by JBoss Application Server, there are three JNDI objects required on the Solace message broker:

* A connection factory: JNDI/Sol/CF
* A queue destination: JNDI/Sol/Q/requests
* A queue destination: JNDI/Sol/Q/replies

They are configured as follows:

Note: this will configure a connection factory without XA support as the default for the XA property is False. See section [Enabling XA Support for JMS Connection Factories](#enabling-xa-support ) for XA configuration.

```
(config)# jndi message-vpn solace_VPN
(config-jndi)# create connection-factory JNDI/Sol/CF
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
(config-jndi)# create queue JNDI/Sol/Q/requests
(config-jndi-queue)# property physical-name solace_requests
(config-jndi-queue)# exit
(config-jndi)#
(config-jndi)# create queue JNDI/Sol/Q/replies
(config-jndi-queue)# property physical-name solace_replies
(config-jndi-queue)# exit
(config-jndi)# 
(config-jndi)# no shutdown
(config-jndi)# exit
(config)#
```

### Step 2: Deploying Solace JCA Resource Adapter

Solace provides a JCA compliant Resource Adapter that can be deployed to the JBoss Application Server to allow Enterprise Java Beans to connect to Solace through a standard JCA interface.  This integration guide outlines the steps required to deploy the Solace resource adapter (provided as a stand-alone RAR file) to JBoss.  

The JBoss EAP is using the [Modular Class Loading mechanism]({{ site.links-jboss-modules }}){:target="_top"} which provides fine-grained isolation of Java classes for deployed applications.  The following deployment instructions provide the steps to deploy the Solace JCA Resource Adapter as a JBoss Global Module.

#### Resource Adapter Deployment Steps

The following steps will make the Solace resource adapter available to all enterprise applications (Refer to the above section [Description of Resources Required](description-of-resources-required) for the file location of the Solace JCA 1.5 Resource Adapter RAR file). 

JBoss allows the developer to configure a specific JCA resource adapter to use for an EJB using either JBoss specific Java annotations or through JBoss deployment descriptor files.  This configuration example makes the Solace JCA Resource Adapter Module available to all EJB applications by configuring it as a Global Module.  Refer to the ‘Class Loading in AS7’ section of the [JBOSS-REF] documentation for further details on JBoss Class-Loading mechanisms.

Steps to deploy the Solace JCA Resource Adapter:

Step 1 - Create a JBoss module directory for the Solace Resource Adapter 

```
<JBoss_Home>/modules/com/solacesystems/ra/main 
```

Step 2 - Copy the Solace JCA 1.5 Resource Adapter RAR file to the module ‘main’ directory and unzip the contents of the RAR file.  Example contents (where ‘xx’ is the software version packaged with the specific RAR file):

```
-rw-r--r-- 1 root root  284220 Jan  1  2015 commons-lang-2.6.jar
-rw-r--r-- 1 root root   62050 Jan  1  2015 commons-logging-1.1.3.jar
drwxr-xr-x 2 root root    4096 Jan  1  2015 META-INF
-rw-r--r-- 1 root root  222446 Jan  1  2015 sol-common-7.1.2.xx.jar
-rw-r--r-- 1 root root 1106969 Jan  1  2015 sol-jcsmp-7.1.2.xx.jar
-rw-r--r-- 1 root root  297726 Jan  1  2015 sol-jms-7.1.2.xx.jar
-rw-r--r-- 1 root root  173579 Jan  1  2015 sol-jms-ra-7.1.2.xx.jar
-rw-r--r-- 1 root root 1965375 Jan  2 15:31 sol-jms-ra-7.1.2.xx.rar
```

Step 3 - In the module ‘main’ directory, create a ‘module.xml’ file that references the JAR libraries in the Solace JCA 1.5 Resource Adapter and specifies other external dependencies.  Example (update the string ‘xx’ with the JAR versions included in the Solace JCA 1.5 Resource Adapter archive):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.3" name="com.solacesystems.ra" >
    <resources>
        <resource-root path="." />
        <resource-root path="commons-lang-2.6.jar" />
        <resource-root path="commons-logging-1.1.3.jar" />
        <resource-root path="sol-jms-10.0.xx.jar" />
        <resource-root path="sol-jms-ra-10.0.xx.jar"/>
    </resources>
    <dependencies>
        <module name="javax.api"/>
        <module name="javax.transaction.api"/>
        <module name="javax.resource.api"/>
        <module name="javax.jms.api" slot="main" export="true"/>
    </dependencies>
</module>
```

When using Solace JMS version 10.0.2 or earlier, the sol-common and sol-jcsmp JARs must be included.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.3" name="com.solacesystems.ra" >
    <resources>
        <resource-root path="." />
        <resource-root path="commons-lang-2.6.jar" />
        <resource-root path="commons-logging-1.1.3.jar" />
        <resource-root path="sol-common-7.1.0.xx.jar" />
        <resource-root path="sol-jcsmp-7.1.0.xx.jar" />
        <resource-root path="sol-jms-7.1.0.xx.jar" />
        <resource-root path="sol-jms-ra-7.1.0.xx.jar"/>
    </resources>
    <dependencies>
        <module name="javax.api"/>
        <module name="javax.transaction.api"/>
        <module name="javax.resource.api"/>
        <module name="javax.jms.api" slot="main" export="true"/>
    </dependencies>
</module>
```

Step 4 - Perform one of the following two steps:

Note: this step is required if using transactions and can be skipped otherwise.

(Option 1) Update the ‘module.xml’ file in the JBoss JTS subsystem to refer to the Solace Resource Adapter module as a dependency (So that the JTS subsystem has access to the classes of the Solace RA for XA Recovery).

* Update the JTS module’s module.xml file which can be found in the following location:

```
<JBoss Home>/modules/system/layers/base/org/jboss/jts/main
```

* Edit the module.xml file and add the com.solacesystems.ra module dependency:

```
:
    <dependencies>
        <module name="org.omg.api" />
        <module name="org.apache.commons.logging"/>
        <module name="org.jboss.logging"/>
        <module name="org.jboss.jts.integration"/>
        <module name="org.jboss.jboss-transaction-spi"/>
        <module name="javax.api"/>
        <module name="javax.transaction.api"/>
        <module name="javax.resource.api"/>
        <module name="org.hornetq"/>
        <module name="org.jacorb"/>
        <module name="org.jboss.genericjms.provider" />

        <module name="com.solacesystems.ra" />
    </dependencies>
:
```

(Option 2) This option avoids the need to modify the JTS subsystem module.xml configuration file.  Using this option exposes the Solace JCA RA Java classes to the JTS subsystem for XA Recovery. 

i.	Start the JBoss Application Server with the following Java system property setting:

```
–Dsoljmsra.classLoaderOverride=true
```

ii.	The above Java system property may be configured in the JBoss application server $JBOSS_HOME/bin/standalone.conf file.  Refer to [JBOSS-REF] for alternate ways to configure these settings depending on your specific server configuration.

Step 5 - Update the JBoss server configuration – ‘urn:jboss:domain:ee’ subsystem to specify the Solace Resource Adapter module as a Global Module:

Note: the version of the subsystem, "V.V" depends on your JBoss EAP version.

```xml
<subsystem xmlns="urn:jboss:domain:ee:V.V">
   <global-modules>
         <module name="com.solacesystems.ra" slot="main"/>
   </global-modules>
   <spec-descriptor-property-replacement>true</spec-descriptor-property-replacement>
   <jboss-descriptor-property-replacement>true</jboss-descriptor-property-replacement>
</subsystem>
```

Step 6 - 1.	Update the JBoss server configuration – ‘urn:jboss:domain:ejb3’ subsystem to specify the Solace Resource Adapter as the default adapter for Message-Driven-Beans:

Note: the version of the subsystem, "V.V" depends on your JBoss EAP version.

```xml
<subsystem xmlns="urn:jboss:domain:ejb3:V.V">
  :
   <mdb>
   	<resource-adapter-ref resource-adapter-name="com.solacesystems.ra"/>
       <bean-instance-pool-ref pool-name="mdb-strict-max-pool"/>
   </mdb>
  :
```

Step 7 - Update the JBoss server configuration – ‘urn:jboss:domain:resource-adapters’ subsystem to add the minimum Solace Resource Adapter configuration.  Note, the resource adapter archive location is specified as a module path ‘com.solacesystems.ra’:

Note: the version of the subsystem, "V.V" depends on your JBoss EAP version.

```xml
<subsystem xmlns="urn:jboss:domain:resource-adapters:V.V">
  <resource-adapters>
     <resource-adapter id="com.solacesystems.ra">
        <module slot="main" id="com.solacesystems.ra"/>
        <transaction-support>XATransaction</transaction-support>
        <config-property name="MessageVPN"/>
        <config-property name="UserName"/>    
        <config-property name="Password"/>
        <config-property name="ConnectionURL"/>
        <config-property name="ExtendedProps"/>
        <connection-definitions/>
        <admin-objects/>
     </resource-adapter>
  </resource-adapters>
</subsystem>
```

### <a name="ConnToSolJMS"></a>Step 3: Connecting to Solace JMS provider

Connecting to the Solace message broker through the Solace JCA Resource Adapter requires configuration of additional resources in JBoss.  Two key pieces of information are required including connectivity information to the Solace message broker and client authentication data.  

The above information is specified across one or more JMS entities depending on your application’s JMS message flow (Inbound, Outbound, or both).  Configuration of a JMS connection factory is required for outbound message flow, while configuration of the Activation Specification associated with a Message-Driven-Bean - is required for inbound message flow.  

The Solace resource adapter includes several custom properties for specifying connectivity and authentication details (Application-Managed credentials) to the Solace message broker.  Setting these properties at the Resource Adapter level makes the information available to all child JCA entities like Connection Factory, Activation Specification and Administered Objects.  The properties can also be overridden at the specific JCA entity level allowing connectivity to multiple Solace message brokers.

#### <a name="RAConf">Steps to configure the Solace JCA Resource Adapter

Step 1 - Update the JBoss server configuration – ‘urn:jboss:domain:resource-adapters’ subsystem and edit the configuration properties of the Solace Resource Adapter.  Update the values for the configuration properties  ‘ConnectionURL’, ‘UserName’, ‘Password’, and ‘MessageVPN’:

Note: ‘ExtendedProps’ is a placeholder for advanced configuration and will not be used here.

```xml
<resource-adapter id="com.solacesystems.ra">
    <module slot="main" id="com.solacesystems.ra"/>
    <transaction-support>XATransaction</transaction-support>
    <config-property name="ConnectionURL">smf://__IP:Port__</config-property>
    <config-property name="MessageVPN">solace_VPN</config-property>
    <config-property name="UserName">UserName</config-property>
    <config-property name="Password">Password</config-property>
    <config-property name="ExtendedProps"></config-property>
    <connection-definitions/>
    <admin-objects/>
</resource-adapter>
```

Step 2 - ‘ConnectionURL’ property has the format ‘smf://__IP:Port__’ (Update the value ‘__IP:Port__’ with the actual Solace message broker message-backbone VRF IP ).

Step 3 - Specify a value for the ‘UserName’ property that corresponds to the Solace username (use the value ‘solace_user’ for this example).

Step 4 - Specify a value for the ‘Password’ property that corresponds to the Solace username’s password, use the value ‘solace_password’ 

Step 5 - Specify a value for the ‘MessageVPN’ property and specify the value corresponding to the Solace message VPN (use the value ‘solace_VPN’ for this example).

The following table summarizes the values used for the Resource Adapter configuration properties.

| **Name** | **Value** | **Description** |
| ConnectionURL | tcp://__IP:Port__ | Update the value '__IP:Port__' with the actual message broker message-backbone VRF IP |
| MessageVPN | solace_VPN | The value corresponding to the Solace message VPN |
| UserName | solace_user | The client Solace username credentials |
| Password | default | Client password |
| ExtendedProps |  | Comma-seperated list for [advanced control of the connection]({{ site.links-docs-jms-properties }}){:target="_top"}.  For this example, leave empty.  Supported values are shown below. |

Extended Properties Supported Values:

* Solace_JMS_Authentication_Scheme 
* Solace_JMS_CompressionLevel 
* Solace_JMS_JNDI_ConnectRetries 
* Solace_JMS_JNDI_ConnectRetriesPerHost 
* Solace_JMS_JNDI_ConnectTimeout 
* Solace_JMS_JNDI_ReadTimeout 
* Solace_JMS_JNDI_ReconnectRetries 
* Solace_JMS_JNDI_ReconnectRetryWait 
* Solace_JMS_SSL_ValidateCertificateDate 
* Solace_JMS_SSL_ValidateCertificate 
* Solace_JMS_SSL_CipherSuites 
* Solace_JMS_SSL_KeyStore 
* Solace_JMS_SSL_KeyStoreFormat 
* Solace_JMS_SSL_KeyStorePassword 
* Solace_JMS_SSL_PrivateKeyAlias 
* Solace_JMS_SSL_PrivateKeyPassword 
* Solace_JMS_SSL_ExcludedProtocols 
* Solace_JMS_SSL_TrustStore 
* Solace_JMS_SSL_TrustStoreFormat 
* Solace_JMS_SSL_TrustStorePassword
* Solace_JMS_SSL_TrustedCommonNameList
* java.naming.factory.initial

For example: 'solace_JMS_CompressionLevel=9'

#### Steps to configure a JCA connection factory

Note: This example is for non-transacted messaging; refer to the section Working with Transactions for details on configuring `<connection-definitions>` for XA enabled JCA connection factories)

Step 1 - Edit the configuration properties of the Solace Resource Adapter in the ‘resource-adapters’ subsystem of the JBoss application server configuration, and add a new connection-definition entry:

```xml
<resource-adapter id="com.solacesystems.ra">
  <module slot="main" id="com.solacesystems.ra"/>
  :
  <connection-definitions>
    <connection-definition 
       class-name = "com.solacesystems.jms.ra.outbound.ManagedJMSConnectionFactory" 
       jndi-name = "java:/jms/myCF" 
       enabled="true" pool-name="myCFPool">

       <config-property name="ConnectionFactoryJndiName">JNDI/Sol/CF</config-property>

       <security>
         <application/>
       </security>
       <validation>
         <background-validation>false</background-validation>
       </validation>
  </connection-definition>
</connection-definitions>
:
</resource-adapter>
```

Step 2 - Specify the value ‘com.solacesystems.jms.ra.outbound.ManagedJMSConnectionFactory‘ for the class-name attribute of the connection-definition.

Step 3 - Edit the local jndi-name attribute of the connection factory as referenced by EJB code (for this example use the value ‘java:/jms/myCF’)

Step 4 - Edit the connection-definition configuration property ‘ConnectionFactoryJndiName’ (for this example use the value ‘JNDI/Sol/CF‘)

Note, values for ConnectionURL, MessageVPN, UserName and Password must also be specified for the JNDI lookup of the connection factory to succeed.  In this example, these values are inherited by the connection-definition from the Resource Adapter configuration properties (or alternatively the values may be specified directly as config-property entries of the JMS connection-definition).

The following table summarizes the values used for the JMS connection factory configuration properties.

| **Name** | **Value** | **Description** |
| ConnectionFactoryJndiName | JNDI/Sol/CF | The JNDI name of the JMS connection factory as configured on the Solace message broker. |

#### Connecting – Sample code 

Sample code for connecting to the Solace message broker through a JCA connection factory will be demonstrated in the section below Step 5 – Sending outbound messages using Solace JMS provider. The sample code in this integration guide is triggered by the receipt of a message by a Message-Driven-Bean (MDB) which in turn calls a Session Bean method to publish an outbound reply message. 

### <a name="RecInbMessages"></a>Step 4: Receiving inbound messages using Solace JMS provider

This example uses a Message-Driven-Bean to receive messages from the Solace JMS provider.  The bean is bound to an Activation Specification which specifies the Solace JMS destination from which to consume messages as well as the authentication details used to connect to the Solace message broker. 

#### Configuration

In JBoss EAP, Message Driven Bean – Activation Specifications are configured using either EJB 3.0 annotations or through EJB deployment descriptor files.  The following example shows the Activation Specification configuration properties available for connecting to a JMS end point on the Solace message broker as well as other configuration options.  

Note: the actual values for the attributes (‘propertyValue’) can take the form of a variable like ‘${propertyName}’ where JBoss replaces the values if the "spec-descriptor-property-replacement" or "jboss-descriptor-property-replacement" JBoss server configuration properties are set to ‘true’ in the ‘urn:jboss:domain:ee’ subsystem (Refer to the [JBoss documentation](https://access.redhat.com/documentation/en-us/jboss_enterprise_application_platform/6.2/html/security_guide/enablingdisabling_descriptor_based_property_replacement1 ) for further details).

```
@MessageDriven(
    activationConfig = {
        @ActivationConfigProperty(
            propertyName="connectionFactoryJndiName",
            propertyValue="JNDI/Sol/CF"),
        @ActivationConfigProperty(
            propertyName="destinationType",
            propertyValue="javax.jms.Queue"),
        @ActivationConfigProperty(
            propertyName="destination", 
            propertyValue="JNDI/Sol/Q/requests"),
        @ActivationConfigProperty(
            propertyName="messageSelector", 
            propertyValue=""),
        @ActivationConfigProperty(
            propertyName="subscriptionDurability",
            propertyValue="Durable"),
        @ActivationConfigProperty(
            propertyName="subscriptionName", 
            propertyValue=""),
        @ActivationConfigProperty(
            propertyName="clientId", 
            propertyValue="ConsumerMDBexample"),
        @ActivationConfigProperty(
            propertyName="batchSize", 
            propertyValue="1"),
        @ActivationConfigProperty(
            propertyName="maxPoolSize", 
            propertyValue="8"),
        @ActivationConfigProperty(
            propertyName="reconnectAttempts", 
            propertyValue="1"),
        @ActivationConfigProperty(
            propertyName="reconnectInterval", 
            propertyValue="30")
    }
)
```

The following activation configuration properties are mandatory:

* connectionFactoryJndiName
* destinationType
* destination

Configuration for this Message-Driven-Bean example:

1. For the ‘connectionFactoryJndiName’ property, specify the value ‘JNDI/Sol/CF’ (this is the value configured on the Solace message broker in section [Setting up Solace JNDI References](#setting-up-solace-jndi-references)).
1. For the ‘destination’ property, specify the value ‘JNDI/Sol/Q/requests’ (the value configured on the Solace message broker in section [Setting up Solace JNDI References](#setting-up-solace-jndi-references).
1. For the ‘destinationType’ property, specify the value ‘javax.jms.Queue’. 

The following table summarizes important values used for the Activation specification configuration properties:

| **Name** | **Value** | **Description** |
| connectionFactoryJndiName | JNDI/Sol/CF | The JNDI name of the JMS connection factory as configured on the message broker. |
| destination | JNDI/Sol/Q/requests | The JNDI name of the JMS destination as configured on the message broker. |
| destinationType | javax.jms.Queue | The JMS class name for the desired destination type. |
| batchSize | 1 | For non-transacted MDBs, the batchSize() is an optimization to read-in 'batchSize' number of messages at a time from the Connection for distribution to MDB threads. Note, for MDB's that are configured to be transacted, the batchSize property is ignored (internally set to '1'). |
| maxPoolSize | 8 | The maximum size of the MDB Session Pool.  One Session services one MDB thread |
| reconnectAttempts | 1 | The number of times to attempt to re-activation of the MDB after activation failure |
| reconnectInterval | 30 | The number of seconds between MDB re-activation attempts |


### Step 5: Sending outbound messages using Solace JMS provider

This example uses an EJB Session Bean to send reply messages using the Solace resource adapter.  The bean code requests a JMS Connection from a Solace Managed Connection Factory (myCF) and then sends a reply message to a JMS destination (myReplyQueue) as configured by a JCA administered object.  

#### Configuration

The connection factory used in this example was configured in section [Connecting to Solace JMS provider](#ConnToSolJMS).  In addition to the connection factory, we must configure a JMS destination for sending reply messages.

Steps to create a JCA administered object (of type Queue)

Step 1 - Edit the Solace Resource Adapter definition in the ‘resource-adapters’ subsystem of the JBoss application server configuration and add a new admin-object entry:

```xml
<resource-adapter id="com.solacesystems.ra">
  <module slot="main" id="com.solacesystems.ra"/>
  :
  <admin-objects>
    <admin-object class-name="com.solacesystems.jms.ra.outbound.QueueProxy" 
        jndi-name="java:/jms/myReplyQueue" 
        enabled="true" use-java-context="false" pool-name="myReplyQueuePool">
      <config-property name="Destination">JNDI/Sol/Q/replies</config-property>
    </admin-object>
  </admin-objects>
  :
</resource-adapter>
```

Step 2 - Specify the value ‘com.solacesystems.jms.ra.outbound.QueueProxy‘ for the class-name attribute of the admin-object.

Step 3 - Edit the local JNDI name attribute value of the admin-object as referenced by EJB application code (for this example use the value ‘java:/jms/myReplyQueue’)

Step 4 - Edit the value for the admin-object configuration property ‘Destination’ (for this example use the value ‘JNDI/Sol/Q/replies‘)

The following table summarizes the values used for the administered object configuration properties:

| **Name** | **Value** | **Description** |
| Destination | JNDI/Sol/Q/replies | The JNDI name of the JMS destination as configured on the message broker. |

## Sample Application Code

Source code for an Enterprise Java Beans (EJB) application for JBoss, implementing waiting for receiving and then sending a message, has been provided in the GitHub repo of this guide.

There are three variants:

* [EJBSample/ejbModule]({{ site.repository }}/blob/master/src/jboss-eap/EJBSample/ejbModule){:target="_blank"} - used in this basic application sample.
* EJBSample-XA-BMT/ejbModule - used in the [Working with Transactions](#working-with-transactions) sample.
* EJBSample-XA-CMT/ejbModule - used in the [Working with Transactions](#working-with-transactions) sample.

The structure of all variants is the same:

* Java source files under `ejbModule/com/solace/sample/`
  * ConsumerMDB.java - implements a message-driven EJB bean, triggered on receiving a message from the Solace message broker
  * ProducerSB.java - implements `sendMessage()` in a session EJB bean, used to send a message to the Solace message broker
  * Producer.java - "Producer"'s remote EJB interface to call `sendMessage()`
  * ProducerLocal.java - "Producer"'s local EJB interface to call `sendMessage()`

* EJB XML descriptor under `ejbModule/META-INF/`
  * ejb-jar.xml - descriptor providing EJB configuration and mappings

### Receiving messages from Solace – Sample Code

The sample code below shows the implementation of a message-driven bean (ConsumerMDB) which listens for JMS messages to arrive on the configured Solace JCA connection factory and destination (`JNDI/Sol/CF` and `JNDI/Sol/Q/requests` respectively - as configured in the Activation specification).  Upon receiving a message, the MDB calls the method sendMessage() of the ProducerSB  session bean which in turn sends a reply message to a ‘reply’ Queue destination.

```java
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "connectionFactoryJndiName", propertyValue = "JNDI/Sol/CF"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "JNDI/Sol/Q/requests") })
public class ConsumerMDB implements MessageListener {

    @EJB(beanName = "ProducerSB", beanInterface = Producer.class)
    Producer sb;

    /**
     * Default constructor.
     */
    public ConsumerMDB() {
    }

    public void onMessage(Message message) {
        String msg = message.toString();

        System.out.println(Thread.currentThread().getName() + " - ConsumerMDB: received message: " + msg);

        try {
            // Send reply message
            sb.sendMessage();
        } catch (JMSException e) {
            throw new EJBException("Error while sending reply message", e);
        }

        System.out.println("Completed processing!");

    }
}
```

The full source code for this example is available in the following source:

 * [ConsumerMDB.java]({{ site.repository }}/blob/master/src/jboss-eap/EJBSample/ejbModule/com/solace/sample/ConsumerMDB.java){:target="_blank"}

### Sending Messages to Solace – Sample code

The sample code below shows the implementation of a session bean ("ProducerSB") that implements a method sendMessage() which sends a JMS message to the Queue destination configured above.  The sendMessage() method is called by the "ConsumerMDB" bean outlined in the previous section.

This example uses Java resource injection for the resources ‘myCF’ and ‘myReplyQueue’ which are mapped to their respective J2C entities using an application binding file (see example application bindings file following the code example below).

```java
@Stateless(name = "ProducerSB")
public class ProducerSB implements Producer, ProducerLocal {
    @Resource(name = "myCF")
    ConnectionFactory myCF;

    @Resource(name = "myReplyQueue")
    Queue myReplyQueue;

    public ProducerSB() {
    }

    @Override
    public void sendMessage() throws JMSException {

        Connection conn = null;
        Session session = null;
        MessageProducer prod = null;

        try {
            conn = myCF.createConnection();
            session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            prod = session.createProducer(myReplyQueue);

            ObjectMessage msg = session.createObjectMessage();
            msg.setObject("Hello world!");
            prod.send(msg, DeliveryMode.PERSISTENT, 0, 0);
        } finally {
            if (prod != null)
                prod.close();
            if (session != null)
                session.close();
            if (conn != null)
                conn.close();
        }
    }
}
```

The sample above requires configuration of JNDI mapped-names to the resource names referenced in the EJB code.  The mapping can be defined in the EJB deployment descriptor file: 

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ejb-jar xmlns="http://java.sun.com/xml/ns/javaee" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd"
	version="3.0">
	<display-name>EJBSample</display-name>
	<enterprise-beans>
		<session>
			<ejb-name>ProducerSB</ejb-name>
			<business-local>com.solace.sample.ProducerLocal</business-local>
			<business-remote>com.solace.sample.Producer</business-remote>
			<ejb-class>com.solace.sample.ProducerSB</ejb-class>
			<session-type>Stateless</session-type>
			<transaction-type>Bean</transaction-type>
			<resource-ref>
				<res-ref-name>myReplyQueue</res-ref-name>
				<res-auth>Application</res-auth>
				<mapped-name>java:/jms/myReplyQueue</mapped-name>
			</resource-ref>
			<resource-ref>
				<res-ref-name>myCF</res-ref-name>
				<res-auth>Application</res-auth>
				<mapped-name>java:/jms/myCF</mapped-name>
			</resource-ref>
		</session>
	</enterprise-beans>
</ejb-jar>
```

The full source code for this example is available in the following sources:

 * [ProducerSB.java]({{ site.repository }}/blob/master/src/jboss-eap/EJBSample/ejbModule/com/solace/sample/ProducerSB.java){:target="_blank"}

### Building the samples

Instructions are provided for "Eclipse IDE for Java EE Developers". Adjust the steps accordingly if your environment differs.

Follow these steps to create and build your project:

1. Clone this project from GitHub
```
git clone https://github.com/SolaceLabs/solace-integration-guides.git
cd solace-integration-guides/src/jboss-eap/EJBSample/ejbModule/
```
1. Create a new "EJB project" in Eclipse, set the target runtime to JBoss EAP. Optionally check the "Add your project to an EAR" to create an Enterprise Archive instead of an EJB JAR.

1. Replace the new project `ejbModule` directory contents (created empty) with the contents of the `ejbModule` directory of this repo, then refresh your project in the IDE.

1. Add JEE API 5 or later jar library to the project build path. This can be your application server's JEE library or download and include a general one, such as from [org.apache.openejb » javaee-api](https://mvnrepository.com/artifact/org.apache.openejb/javaee-api ).

1. Export your project to an EJB JAR file or alternatively, if you have a related EAR project created then export from there to an EAR file.

You have now built the sample application as a deployable JAR or EAR archive. Take note of the file and directory location.

### Deploying the sample application

Steps to deploy the sample application, assuming the "standalone" mode clustering is used:

1. Place your JAR or EAR file in the $JBOSS_HOME/standalone/deployments folder

1. Ensure your JBoss EAP application server is running.

You have now deployed the sample application and it is ready to receive messages from the `solace_requests` queue on the message broker.

### Testing the sample application

To send a test message you can use the **queueProducerJNDI** sample application from the [Obtaining JMS objects using JNDI](https://dev.solace.com/samples/solace-samples-jms/using-jndi/ ) tutorial. Ensure to adjust in the source code the "CONNECTION_FACTORY_JNDI_NAME" and "QUEUE_JNDI_NAME" to `JNDI/Sol/CF` and `JNDI/Sol/Q/requests` respectively, as used in this tutorial.

Once a message has been sent to the `solace_requests` queue it will be delivered to the enterprise application, which will consume it from there and send a new message to the `solace_replies` queue.

You can check the messages in the `solace_replies` queue using the using [Solace PubSub+ Manager]({{ site.links-docs-webadmin }}){:target="_top"}, Solace's browser-based administration console.

You can also check how the message has been processed in JBoss logs as described in section [Debugging Tips](#debugging-tips-for-solace-jms-api-integration).
 
## Performance Considerations<a name="performance-considerations"></a>

The Solace JCA Resource Adapter relies on the JBoss Application Server for managing the pool of JCA connections.  Tuning performance for outbound messaging can in part be accomplished by balancing the maximum number of pooled connections available against the number of peak concurrent outbound messaging clients. 

For inbound messaging there are different levers that can be tuned for maximum performance in a given environment.  The ‘batchSize’ configuration property of the Solace - Activation Specification (AS) defines the maximum number of messages retrieved at a time from a JMS destination for delivery to a server session.  The server session then routes those messages to respective Message Driven Beans.  In addition, the ‘maxPoolSize’ configuration property of the Solace AS defines the maximum number of pooled JMS sessions that can be allocated to MDB threads.  Therefore to fine tune performance for inbound messaging, the ‘batchSize’ and ‘maxPoolSize’ must be balanced to the rate of incoming messages.

Another consideration is the overhead of performing JNDI lookups to the Solace message broker.  JBoss implements JNDI caching by default.  Resources referenced by a Message Driven Bean through resource injection will trigger an initial JNDI lookup and subsequently use the cached information whenever the MDB instance is reused from the MDB pool. Similarly, Session beans that perform JNDI lookups through a JNDI Context will have that information cached in association with that context.  Whenever the Session bean instance is reused from the Session bean pool, any lookups using the same JNDI Context will utilize the JNDI cached information.  

Note, in order to use the JNDI caching mechanisms within JBoss you must use JMS through a JCA resource adapter and reference JMS end points in your code through JEE resource injection. 

Please refer to [JBOSS-REF] for details on modifying the default behavior of JNDI caching.

## Working with Solace High Availability (HA)<a name="working-with-solace-high-availability-ha"></a>

The [Solace Messaging API for JMS]({{ site.links-docs-jms }}){:target="_top"} section "Establishing Connection and Creating Sessions" provides details on how to enable the Solace JMS connection to automatically reconnect to the standby message broker in the case of a HA failover of a message broker. By default Solace JMS connections will reconnect to the standby message broker in the case of an HA failover.

In general the Solace documentation contains the following note regarding reconnection:

Note: When using HA redundant message brokers, a fail-over from one message broker to its mate will typically occur in less than 30 seconds, however, applications should attempt to reconnect for at least five minutes. 

In "Setting up Solace JNDI References", the Solace CLI commands correctly configured the required JNDI properties to reasonable values. These commands are repeated here for completeness.

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

In addition to configuring the above properties for connection factories, care should be taken to configure connection properties for performing JNDI lookups to the Solace message broker.  These settings can be configured in the JBoss Application Server globally by setting them at the Solace resource adapter level or within individual JCA administered objects.  

To configure JNDI connection properties for JNDI lookups, set the corresponding Solace JMS property values (as a semi-colon separated list of name=value pairs) through the ‘ExtendedProps’ configuration property of the Solace resource adapter or JCA administered objects.

* Solace_JMS_JNDI_ConnectRetries = 1
* Solace_JMS_JNDI_ConnectRetriesPerHost = 5
* Solace_JMS_JNDI_ConnectTimeout = 30000 (milliseconds)
* Solace_JMS_JNDI_ReadTimeout = 10000 (milliseconds)
* Solace_JMS_JNDI_ReconnectRetries = 20 
* Solace_JMS_JNDI_ReconnectRetryWait 3000 (milliseconds)

## Debugging Tips for Solace JMS API Integration<a name="debugging-tips-for-solace-jms-api-integration"></a>

The key component for debugging integration issues with the Solace JMS API is to enable API logging. Enabling API logging from JBoss Application Server is described below.

### How to enable Solace JMS API logging

Logging and the logging levels for Solace Resource Adapter Java packages can be enabled using Log4J style configuration in the JBoss ‘urn:jboss:domain:logging’ subsystem.  You can enable logging for one or more of the Solace Resource Adapter Java packages listed below.

Note the trace logs can be found in the JEE server logs directory (example: $JBOSS_HOME/standalone/server.log).

Steps to configure debug tracing for specific Solace API packages:

Step 1 - Modify the JBoss server configuration:

* In the subsystem ‘urn:jboss:domain:logging’, add  entries for one or more of the following Solace Resource Adapter packages (Update the logging level to one of ‘FATAL’, ‘ERROR’, ‘WARN’, ‘INFO’, ‘DEBUG’, or ‘TRACE’).

Note: the version of the subsystem, “V.V” depends on your JBoss EAP version.

```xml
<subsystem xmlns="urn:jboss:domain:logging:V.V">
:
  <logger category="com.solacesystems.jms">
<level name="INFO"/>
  </logger>
  <logger category="com.solacesystems.jndi">
       <level name="INFO"/>
  </logger>
  <logger category="com.solacesystems.jcsmp">
       <level name="INFO"/>
  </logger>
  <logger category="com.solacesystems.common">
       <level name="INFO"/>
  </logger>
:
```

## Advanced Topics

### Authentication

The integration example illustrated in [Connecting to Solace JMS provider](#ConnToSolJMS) of this guide uses the authentication information specified in the custom properties of the Solace resource adapter.  These authentication properties are used whenever Application Managed authentication is specified for a JCA resource.  No matter the authentication mode (Application-Managed or Container-Managed) specified for a resource, the Solace ‘MessageVPN’ information for a connection is always retrieved from the Solace resource adapter configured properties (or from the configured properties of one of the JCA entities – connection factory, administered object or activation specification).

JBoss supports configuration of Container-Managed authentication for JCA connection factories.  The JAAS login module ConfiguredIdentityLoginModule can be used to provide EJB Container-supplied sign-on credentials to the Solace message broker. Refer to [JBOSS-SEC] for more details on configuring EJB Security.

The message broker supports a variety of client authentications schemes as described in the Solace documentation [Client Authentication and Authorization]({{ site.links-docs-client-authenticate-authorize }}){:target="_top"}.  The Solace JCA resource adapter supports a subset of these schemes including `Basic` authentication and 'SSL Client Certificate' authentication.  The default authentication scheme used by the Solace JMS Resource Adapter is AUTHENTICATION_SCHEME_BASIC.

The value of the Solace Resource Adapter custom property 'extendedProps' is used to specify an alternate authentication scheme such as 'AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE'. The value of the custom property 'extendedProps' consists of a semi-colon separated list of Solace JMS property / value pairs (SOLACE_PROPERTY=value).  You can specify the required properties for an alternate authentication scheme using this technique.  Refer to the [Solace JMS API Online Reference Documentation]({{ site.links-docs-jms-api }}){:target="_top"} for further details on the required JMS properties for configuring SSL client certificate authentication.

Although the authentication scheme AUTHENTICATION_SCHEME_BASIC is the default scheme, that scheme could also have been specified using the `extendedProps` custom property of the resource adapter.

* ExtendedProps - solace_JMS_Authentication_Scheme=AUTHENTICATION_SCHEME_BASIC

### Using SSL Communication<a name="using-ssl-communication"></a>

This section outlines how to update the Solace message broker and JBoss Application Server configuration to switch the client connection to using secure connections with the Solace message broker. For the purposes of illustration, this section uses a server certificate on the Solace message broker and basic client authentication. It is possible to configure Solace JMS to use client certificates instead of basic authentication. This is done using configuration steps that are very similar to those outlined in this document. The [Solace-Docs] and [Solace-JMS-REF] outline the extra configuration items required to switch from basic authentication to client certificates.

To change a JBoss Application Server from using a plain text connection to a secure connection, first the Solace message broker configuration, then the Solace JMS configuration within the JBoss Application Server must be updated as outlined in the next sections.

#### Configuring the Solace Message Broker

To enable secure connections to the Solace message broker, the following configuration must be updated on the Solace message broker.

* Server Certificate
* TLS/SSL Service Listen Port
* Enable TLS/SSL over SMF in the Message VPN

The following sections outline how to configure these items.

##### Configure the Server Certificate

Before starting, here is some background information on the server certificate required by the message broker. This is from the [Solace documentation](https://docs.solace.com/Configuring-and-Managing/Managing-Server-Certs.htm ):

```
To enable TLS/SSL-encryption, you must set the TLS/SSL server certificate file that the Solace PubSub+ message broker is to use. This server certificate is presented to clients during TLS/SSL handshakes. The server certificate must be an x509v3 certificate and include a private key. The server certificate and key use an RSA algorithm for private key generation, encryption and decryption, and they both must be encoded with a Privacy Enhanced Mail (PEM) format.
```

To configure the server certificate, first copy the server certificate to the Solace message broker. For the purposes of this example, assume the server certificate file is named "mycert.pem".

```
# copy sftp://[<username>@]<ip-addr>/<remote-pathname>/mycert.pem /certs
<username>@<ip-addr>'s password:
#
```

Then set the server certificate for the Solace message broker.

```
(config)# ssl server-certificate mycert.pem
(config)#
```

##### Configure TLS/SSL Service Listen Port

By default, the Solace message broker accepts secure messaging client connections on port 55443. If this port is acceptable then no further configuration is required and this section can be skipped. If a non-default port is desired, then follow the steps below. Note this configuration change will disrupt service to all clients of the Solace message broker and should therefore be performed during a maintenance window when this client disconnection is acceptable. This example assumes that the new port should be 55403.

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

###### Enable TLS/SSL within the Message VPN 

By default within Solace message VPNs both the plain-text and SSL services are enabled. If the Message VPN defaults remain unchanged, then this section can be skipped. However, if within the current application VPN, this service has been disabled, then for secure communication to succeed it should be enabled. The steps below show how to enable SSL within the SMF service to allow secure client connections from the JBoss Application Server. 

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

#### Configuring the JBoss Application Server

Secure connections to the Solace JMS provider require configuring SSL parameters of JCA objects. Two of these configuration parameters include ‘ConnectionURL’ and ‘ExtendedProps’.  Note that the property values for ‘ConnectionURL’ and ‘ExtendedProps’ are inherited by JCA connection factory, Activation specification, and administered objects from their parent Resource Adapter.  Thus, unless you are connecting to multiple Solace message brokers, a best practice is to configure values for ‘ConnectionURL’ and ‘ExtendedProps’ in the Solace Resource Adapter, otherwise the SSL related changes should be duplicated across configuration properties for all of the JMS administered objects you want to secure. 

The required SSL parameters include modifications to the URL scheme of ‘ConnectionURL’ (from ‘smf’ to ‘smfs’), and setting additional SSL attributes through the configuration property ‘ExtendedProps’.  The following sections describe the required changes in more detail.

##### Updating the JMS provider URL (ConnectionURL)

In order to signal to the Solace JMS API that the connection should be a secure connection, the protocol must be updated in the URI scheme. The Solace JMS API has a URI format as follows:

```
<URI Scheme>://[username]:[password]@<IP address>[:port]
```

Recall from section [Connecting to Solace JMS provider](#ConnToSolJMS), originally, the "ConnectionURL" was as follows:

```
smf://___IP:PORT___
```

This specified a URI scheme of "smf" which is the plaint-text method of communicating with the Solace message broker. This should be updated to "smfs" to switch to secure communication giving you the following configuration:

```
smfs://___IP:PORT___
```

Steps to update the ConnectionURL configuration property of a Solace JMS Resource Adapter:

Step 1 - Update the JBoss server configuration – ‘urn:jboss:domain:resource-adapters’ subsystem and edit the configuration properties of the Solace Resource Adapter.  Update the values for the configuration properties  ‘ConnectionURL’:

```xml
<resource-adapter id="com.solacesystems.ra">
    <module slot="main" id="com.solacesystems.ra"/>
    :
    <config-property name="ConnectionURL">smfs://__IP:Port__</config-property>
    
    <config-property name="MessageVPN">solace_VPN</config-property>
    <config-property name="UserName">solace_user</config-property>
    <config-property name="Password">Password</config-property>
    <connection-definitions/>
    <admin-objects/>
</resource-adapter>
```

Step 2 - ‘ConnectionURL’ property has the format ‘smfs://__IP:Port__’ (Update the value ‘__IP:Port__’ with the actual Solace message broker message-backbone VRF IP and SMF SSL Port #, note the default SSL Port is ‘55443).

##### Specifying other SSL Related Configuration

The Solace JMS API must be able to validate the server certificate of the Solace message broker in order to establish a secure connection. To do this, the following trust store parameters need to be provided.

First the Solace JMS API must be given a location of a trust store file so that it can verify the credentials of the Solace message broker server certificate during connection establishment. This parameter takes a URL or Path to the trust store file.  

A value for the parameter ‘Solace_JMS_SSL_TrustStore’ can be set by modifying the Solace JCA Resource Adapter configuration property ‘ExtendedProps’. The configuration property value for ‘ExtendedProps’ is comprised of a semi-colon separated list of Solace JMS parameters:

```
Solace_JMS_SSL_TrustStore=___Path_or_URL___ 
```

A trust store password may also be specified. This password allows the Solace JMS API to validate the integrity of the contents of the trust store. This is done through the Solace JMS parameter ‘Solace_JMS_SSL_TrustStorePassword’.

```
Solace_JMS_SSL_TrustStorePassword=___Password___
```

There are multiple formats for the trust store file. By default Solace JMS assumes a format of Java Key Store (JKS). So if the trust store file follows the JKS format then this parameter may be omitted. Solace JMS supports two formats for the trust store: "jks" for Java Key Store or "pkcs12". Setting the trust store format is done through the parameter ‘Solace_JMS_SSL_TrustStoreFormat’:

```
Solace_JMS_SSL_TrustStoreFormat=jks
```

In a similar fashion, the authentication scheme used to connect to Solace may be specified using the parameter ‘Solace_JMS_Authentication_Scheme’ (Please refer to the document [Solace-JMS-REF] for full list of supported extended parameters):

* AUTHENTICATION_SCHEME_BASIC 
* AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE

The integration examples in this guide use basic authentication (the default authentication scheme):

```
Solace_JMS_Authentication_Scheme=AUTHENTICATION_SCHEME_BASIC
```

The following example allows SSL connectivity for connections made through a Solace JCA managed connection factory.  

Steps to update the ‘ExtendedProps’ configuration property of JMS connection factory:

Step 1 - Edit the configuration properties of the Solace Resource Adapter in the ‘urn:jboss:domain:resource-adapters’ subsystem of the JBoss application server configuration and add or update a ‘config-property’ entry for the configuration property ’ExtendedProps’ for a specific JMS connection factory:

```xml
<resource-adapter id="com.solacesystems.ra">
  :
  <connection-definitions>
    <connection-definition 
       class-name = "com.solacesystems.jms.ra.outbound.ManagedJMSConnectionFactory" 
       jndi-name = "java:/jms/myCF" 
       enabled="true" pool-name="myCFPool">
       <config-property name="ConnectionFactoryJndiName">JNDI/Sol/CF</config-property>
       <config-property name="ExtendedProps"> Solace_JMS_SSL_TrustStore=___Path_or_URL___;Solace_JMS_SSL_TrustStorePassword=___Password___;Solace_JMS_SSL_TrustStoreFormat=jks 
       </config-property>
       <security>
         <application/>
       </security>
       <validation>
         <background-validation>false</background-validation>
       </validation>
    </connection-definition>
  </connection-definitions>
</resource-adapter>
```

Step 2 - Specify or supplement the value for the ‘ExtendedProps’ configuration property to: ‘Solace_JMS_SSL_TrustStore=___Path_or_URL___;Solace_JMS_SSL_TrustStorePassword=___Password___;Solace_JMS_SSL_TrustStoreFormat=jks’  (Update the values ‘__Path_or_URL__’ and ‘__Password__’ accordingly)

### Working with Transactions<a name="working-with-transactions"></a>

This section demonstrates how to configure the Solace message broker to support the transaction processing capabilities of the Solace JCA Resource Adapter.  In addition, code examples are provided showing JMS message consumption and production over both types of Enterprise Java Bean transactions: Container-Managed-Transactions (CMT) and Bean-Managed-Transaction (BMT) configuration.

Both BMT and CMT transactions are mapped to Solace JCA Resource Adapter XA Transactions. XA transactions are supported from the general-availability release of SolOS version 7.1.

Note: BMT is using one-phase-commit and for CMT it is up to the container to use one-phase or two-phase-commit.

In addition to the standard XA Recovery functionality provided through the Solace JCA Resource Adapter, the Solace message broker provides XA transaction administration facilities in the event that customers must perform manual failure recovery. For full details refer to the [Solace documentation on administering and configuring XA Transaction](https://docs.solace.com/Configuring-and-Managing/Performing-Heuristic-Actions.htm ) on the Solace message broker.

#### Enabling XA Support for JMS Connection Factories – Solace Message Broker<a name="enabling-xa-support"></a>

When using CMT or BMT transactions, XA transaction support must be enabled for the specific JMS connection factories: the customer needs to configure XA support property for the respective JNDI connection factory on the Solace message broker:

```
(config)# jndi message-vpn solace_VPN
(config-jndi)# connection-factory JNDI/Sol/CF
(config-jndi-connection-factory)# property-list messaging-properties
(config-jndi-connection-factory-pl)# property xa true
(config-jndi-connection-factory-pl)# exit
(config-jndi-connection-factory)# exit
(config-jndi)#
```

#### Enabling XA Recovery Support for JCA Connection Factories – JBoss

To enable XA Recovery for specific JCA connection factories in JBoss the customer must update the connection factory definition with the Solace message broker sign-on credentials that will be used by the JTS subsystem during XA-recovery.  In addition the customer may also want to modify XA connection pool settings from default values.

Steps to enable XA-recovery for a JCA connection factory:

Step 1 - Edit the configuration properties of the Solace Resource Adapter in the ‘urn:jboss:domain:resource-adapters’ subsystem of the JBoss application server configuration and add or update the ‘recovery’ sign-on credentials.  The user-name and password values may be specified using replaceable JBoss property names (Example: ‘${solace.recovery.user}’).  Note the property ‘solace.recovery.user’ may be defined in the JBoss Server Bootstrap Script Configuration file (Example: <JBOSS_HOME>/bin/standalone.conf by setting JAVA_OPTS="$JAVA_OPTS –Dsolace.recovery.user=solace_user"):

```xml
  <connection-definitions>
    <connection-definition 
       class-name = "com.solacesystems.jms.ra.outbound.ManagedJMSConnectionFactory" 
       jndi-name = "java:/jms/myCF" 
       enabled="true" pool-name="myCFXAPool">
       <config-property name="ConnectionFactoryJndiName">JNDI/Sol/CF</config-property>
       <xa-pool>
         <min-pool-size>0</min-pool-size>
         <max-pool-size>10</max-pool-size>
         <prefill>false</prefill>
         <use-strict-min>false</use-strict-min> 
         <flush-strategy>FailingConnectionOnly</flush-strategy>
         <pad-xid>false</pad-xid>
         <wrap-xa-resource>true</wrap-xa-resource>
       </xa-pool>
       </config-property>
       <security>
         <application/>
       </security>
       <validation>
         <background-validation>false</background-validation>
       </validation>
       <recovery>
         <recover-credential>
           <user-name>${solace.recovery.user}</user-name>
           <password>${solace.recovery.password}</password>
         </recover-credential>
       </recovery>
    </connection-definition>
  </connection-definitions>
```

#### Transactions – Sample Code

The following examples demonstrate how to receive and send messages using EJB transactions. Examples are given for both BMT and CMT in GitHub:

* [EJBSample-XA-BMT/ejbModule]({{ site.repository }}/blob/master/src/jboss-eap/EJBSample-XA-BMT/ejbModule/){:target="_blank"}
* [EJBSample-XA-CMT/ejbModule]({{ site.repository }}/blob/master/src/jboss-eap/EJBSample-XA-CMT/ejbModule/){:target="_blank"}

For building and deployment instructions refer to the [Sample Application Code](#building-the-samples) section.

##### Receiving messages from Solace over XA transaction – CMT Sample Code

The following code is similar to the basic "ConsumerMDB" example, but specifies Container-Managed XA Transaction support for inbound messages - see in the `@TransactionManagement` and `@TransactionAttribute` annotations.  In this example, the Message-Driven-Bean (MDB) - 'XAConsumerMDB' is configured such that the EJB container will provision and start an XA transaction prior to calling the onMessage() method and finalize or rollback the transaction when onMessage() exits (Rollback typically occurs when an unchecked exception is caught by the Container).

```java
@TransactionManagement(value = TransactionManagementType.CONTAINER)

@MessageDriven(activationConfig = {
    ... }   // skipping details here
public class XAConsumerMDB implements MessageListener {

    @EJB(beanName = "XAProducerSB", beanInterface = Producer.class)
    Producer sb;

    public XAConsumerMDB() { }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRED)
    public void onMessage(Message message) {
        :
        :
    }  
}
```

The full source code for this example is available here:

*    [XAConsumerMDB.java]({{ site.repository }}/blob/master/src/jboss-eap/EJBSample-XA-CMT/ejbModule/com/solace/sample/XAConsumerMDB.java){:target="_blank"}

##### Sending Messages to Solace over XA Transaction – CMT Sample Code

The following code is similar to the EJB example from section [Sending Messages to Solace – Sample code](#sending-messages-to-solace) but configures Container-Managed XA Transaction support for outbound messaging.  In this example, the Session Bean ‘XAProducerSB’ method ‘SendMessage()’ requires that the caller have an existing XA Transaction context.  In this example, the ‘SendMessage()’ method is called from the MDB - ‘XAConsumerMDB’ in the above example where the EJB container has created an XA Transaction context for the inbound message.  When the method sendMessage() completes the EJB container will either finalize the XA transaction or perform a rollback operation.

```java
@Stateless(name = "ProducerSB")
@TransactionManagement(value = TransactionManagementType.CONTAINER)
public class XAProducerSB implements Producer, ProducerLocal {

    @Resource(name = "myCF")
    ConnectionFactory myCF;

    @Resource(name = "myReplyQueue")
    Queue myReplyQueue;

    public XAProducerSB() {
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRED)
    @Override
    public void sendMessage() throws JMSException {
        :
        :
}
```

    
The full source code for this example is available here:

* [XAProducerSB.java]({{ site.repository }}/blob/master/src/jboss-eap/EJBSample-XA-CMT/ejbModule/com/solace/sample/XAProducerSB.java){:target="_blank"}


##### Sending Messages to Solace over XA Transaction – BMT Sample Code

EJB code can use the UserTransaction interface (Bean-Managed) to provision and control the lifecycle of an XA transaction.  The EJB container will not provision XA transactions when the EJB class’s ‘TransactionManagement’ type is designated as ‘BEAN’ managed.  In the following example, the session Bean ‘XAProducerBMTSB’ starts a new XA Transaction and performs an explicit ‘commit()’ operation after successfully sending the message.  If a runtime error is detected, then an explicit ‘rollback()’ operation is executed.  If the rollback operation fails, then the EJB code throws an EJBException() to allow the EJB container to handle the error.  

```java
@Stateless(name = "XAProducerBMTSB")
@TransactionManagement(value=TransactionManagementType.BEAN)
public class XAProducerBMTSB implements Producer, ProducerLocal {
    @Resource(name = "myCF")
    ConnectionFactory myCF;

    @Resource(name = "myReplyQueue")
    Queue myReplyQueue;

    @Resource
    SessionContext sessionContext;

    public XAProducerBMTSB() {
    }

    @Override
    public void sendMessage() throws JMSException {
        :
        :
        UserTransaction ux = sessionContext.getUserTransaction();
        
        try {
            ux.begin();
            conn = myCF.createConnection();
            session = conn.createSession(true, Session.AUTO_ACKNOWLEDGE);
            prod = session.createProducer(myReplyQueue);
            ObjectMessage msg = session.createObjectMessage();
            msg.setObject("Hello world!");
            prod.send(msg, DeliveryMode.PERSISTENT, 0, 0);          
            ux.commit();
        } catch (Exception e) {
            e.printStackTrace();
            try {
               ux.rollback();
            } catch (Exception ex) {
               throw new EJBException(
                "rollback failed: " + ex.getMessage(), ex);
            }
	}}
```
    
The full source code for this example is available here:

*    [XAProducerBMTSB.java]({{ site.repository }}/blob/master/src/jboss-eap/EJBSample-XA-BMT/ejbModule/com/solace/sample/XAProducerBMTSB.java){:target="_blank"}




### Working with Solace Disaster Recovery<a name="working-with-solace-disaster-recovery"></a>

The [Solace-FG] section "Data Center Replication" contains a sub-section on "Application Implementation" which details items that need to be considered when working with Solace’s Data Center Replication feature. This integration guide will show how the following items required to have a JBoss Application Server successfully connect to a backup data center using the Solace Data Center Replication feature.

* Configuring a Host List within the JBoss Application Server
* Configuring JMS Reconnection Properties within Solace JNDI
* Configuring Message Driven Bean Re-activation in the Event of Activation Failures
* Disaster Recovery Behavior Notes

#### Configuring a Host List within the JBoss Application Server

As described in [Solace-Docs], the host list provides the address of the backup data center. This is configured within the JBoss Application Server through the ConnectionURL configuration property value (of a respective JCA entity) as follows:

```
smf://__IP_active_site:PORT__,smf://__IP_standby_site:PORT__
```

The active site and standby site addresses are provided as a comma-separated list of ‘Connection URIs’.  When connecting, the Solace JMS connection will first try the active site and if it is unable to successfully connect to the active site, then it will try the standby site. This is discussed in much more detail in the referenced Solace documentation

#### Configuring reasonable JMS Reconnection Properties within Solace JNDI

In order to enable applications to successfully reconnect to the standby site in the event of a data center failure, it is required that the Solace JMS connection be configured to attempt connection reconnection for a sufficiently long time to enable the manual switch-over to occur. This time is application specific depending on individual disaster recovery procedures and can range from minutes to hours depending on the application. In general it is best to tune the reconnection by changing the "reconnect retries" parameter within the Solace JNDI to a value large enough to cover the maximum time to detect and execute a disaster recovery switch over. If this time is unknown, it is also possible to use a value of "-1" to force the Solace JMS API to reconnect indefinitely.

The reconnect retries is tuned in the Solace message broker CLI as follows:

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

#### Configuring Message Driven Bean Reactivation in the Event of Activation Failures

If a message driven bean is de-activated during a replication failover, the bean may be successfully re-activated to the replication site if the reconnection properties of the bean’s Activation Specification are properly configured.  The default reconnection properties of the Activation specification are configured to not re-activate the bean upon de-activation.  

To enable JBoss to attempt to re-activate the de-activated MDB, configure the reconnection configuration properties of the Activation specification:

| **Custom Property** | **Default Value** | **Description** |
| reconnectAttempts | 0 | The number of times to attempt to re-activate an MDB after the MDB has failed to activate. |
| reconnectInterval | 10 | The time interval in seconds to wait between attempts to re-activate the MDB. |

##### Receiving Messages in a Message Driven Bean

There is no special processing required during a disaster recovery switch-over specifically for applications receiving messages. After successfully reconnecting to the standby site, it is possible that the application will receive some duplicate messages. The application should apply normal duplicate detection handling for these messages.

##### Sending Messages from a Session Bean

For JBoss applications that are sending messages, there is nothing specifically required to reconnect the Solace JMS connection. However, any messages that were in the process of being sent will receive an error from the Solace Resource Adapter.  These messages must be retransmitted as possibly duplicated. The application should catch and handle any of the following exceptions:

* javax.resource.spi.SecurityException
* javax.resource.ResourceException or one of its subclasses
* javax.jms.JMSException


### Using an external JNDI store for Solace JNDI lookups<a name="using-an-external-jndi-store-for-solace-jndi-lookups"></a>

By default the Solace JMS Resource Adapter looks up JNDI objects, which are the Connection Factory and Destination Objects, from the JNDI store on the message broker.

It's possible to use an external JNDI provider such as an external LDAP server instead, and provision the Solace JNDI objects there.

The following configuration changes are required to use an external JNDI provider:

##### Solace JMS Resource Adapter configuration

Update the `<config-property>` entries under the Solace Resource Adapter in the JBoss server configuration – ‘urn:jboss:domain:resource-adapters’ subsystem.

Refer to section [Solace Resource Adapter configuration section]((#RAConf) to compare to the default setup.

The following table summarizes the values used for the resource adapter’s bean properties if using an external JNDI store:

| **Name** | **Value** | **Description** |
| ConnectionURL | PROVIDER_PROTOCOL://IP:Port | The JNDI provider connection URL (Update the value with the actual protocol, IP and port). Example: `ldap://localhost:10389/o=solacedotcom` |
| MessageVPN |  | The associated solace message VPN for Connection Factory or Destination Objectis is expected to be stored in the external JNDI store. |
| UserName | jndi_provider_username | The username credential on the external JNDI store (not on the message broker) |
| Password | jndi_provider_password | The password credential on the external JNDI store (not on the message broker) |
| ExtendedProps | java.naming.factory.initial= PROVIDER_InitialContextFactory_CLASSNAME (ensure there is no space used around the = sign) | Substitute `PROVIDER_InitialContextFactory_CLASSNAME` with your provider's class name implementing "javax.naming.spi.InitialContextFactory". Example: `com.sun.jndi.ldap.LdapCtxFactory`. Additional Extended Properties Supported Values may be configured as described in the [Solace Resource Adapter configuration section](#RAConf).

**Important note**: the jar library with the 3rd party provider's implementation of the  "javax.naming.spi.InitialContextFactory" class must be placed in the application server's class path. For JBoss EAP v6 the recommended approach is to make use of [JBoss Modules]({{ site.links-jboss-modules }}){:target="_top"}: declare a dependency from Solace Resource Adapter module file (`<JBoss_Home>/modules/com/solacesystems/ra/main/module.xml`) to the module where the jar library is located. Example for the LDAP `com.sun.jndi.ldap.LdapCtxFactory`:

```
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.1" name="com.solacesystems.ra" >
    <resources>
        :
    </resources>
    <dependencies>
        :
        <module name="sun.jdk"/>
    </dependencies>
</module>
```

In this case the implementation jar library is already located in the standard included "sun.jdk" module under `<JBoss_Home>/modules/system/layers/base`, at `sun\jdk`. For libraries that are not included in JBoss, define a module in the [Filesystem module repository]({{ site.links-jboss-modules-file-system }}){:target="_top"}.


<br/>

##### Connection Factories and Administered Objects configuration

Update `<connection-definitions>` and `<admin-objects>` sections under the Solace Resource Adapter in the JBoss server configuration – ‘urn:jboss:domain:resource-adapters’ subsystem.

Refer to sections [Connecting to Solace JMS provider](#ConnToSolJMS) and [Receiving inbound messages using Solace JMS provider](#RecInbMessages) to compare to the default setup.

The following table summarizes the values used for custom properties if using an external JNDI store:

| **config-property Name** | **Value** | **Description** |
| connectionFactoryJndiName | \<CONFIGURED_CF_JNDI_NAME\> | The JNDI name of the JMS connection factory as configured on the external JNDI store. |
| destination | \<CONFIGURED_REPLY_QUEUE_JNDI_NAME\> | The JNDI name of the JMS destination as configured on the external JNDI store. |

Where \<NAME\> is the configured defined name for the object in the external JNDI store.

##### Activation Specification configuration

Update the Activation Configuration in the Message Driven Bean source code ("ConsumerMDB.java") with the defined \<NAME\> for the object in the external JNDI store:

```java
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "connectionFactoryJndiName", propertyValue = "<CONFIGURED_CF_JNDI_NAME>"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "<CONFIGURED_REQUEST_QUEUE_JNDI_NAME>") })
```


## Configuration Reference

There are some associated files you can use for reference:
*    [ProducerSB.java]({{ site.repository }}/blob/master/src/jboss-eap/ProducerSB.java){:target="_blank"}
*    [XAProducerSB.java]({{ site.repository }}/blob/master/src/jboss-eap/XAProducerSB.java){:target="_blank"}
*    [XAProducerBMTSB.java]({{ site.repository }}/blob/master/src/jboss-eap/XAProducerBMTSB.java){:target="_blank"}
*    [ConsumerMDB.java]({{ site.repository }}/blob/master/src/jboss-eap/ConsumerMDB.java){:target="_blank"}
*    [XAConsumerMDB.java]({{ site.repository }}/blob/master/src/jboss-eap/XAConsumerMDB.java){:target="_blank"}
*    [ejb-jar.xml]({{ site.repository }}/blob/master/src/jboss-eap/ejb-jar.xml){:target="_blank"}


