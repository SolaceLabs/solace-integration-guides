---
layout: guides
title: WebSphere Application Server V7 and V8
summary: The WebSphere Application Server provides a comprehensive framework for application and integration middleware that is compliant with the Java Enterprise Edition computing platform. Solace provides a Java Connector Architecture (JCA) compliant Resource Adapter that may be deployed to the WebSphere application server providing enterprise applications with connectivity to the Solace PubSub+ message broker.
icon: ibm-websphere.png
links:
   - label: Example Source Code - WebSphere
     link: https://github.com/SolaceLabs/solace-integration-guides/blob/master/src/websphere
---

## Overview

This document shows you how to integrate the Solace Java Message Service (JMS) with the WebSphere Application Server V7 and V8 for production and consumption of JMS messages. We'll outline best practices to allow efficient use of both the application server and Solace JMS.

The target audience is developers using the WebSphere Application Server who have general knowledge of both it and JMS. This document focuses on the steps required to integrate the two, and provides instructions on configuring and deploying the Solace JCA 1.5 resource adapter using the web console application of WebSphere versions 7 and 8.  For detailed information on either Solace JMS or the WebSphere Application Server, refer to the documents referenced below.

This document is divided into the following sections:

* [Integrating with WebSphere Application Server](#integrating-with-websphere-application-server)
* [Performance Considerations](#performance-considerations)
* [Working with Solace High Availability](#working-with-solace-high-availability-ha)
* [Debugging Tips ](#debugging-tips-for-solace-jms-api-integration)
* Advanced Topics including:
  * [Using SSL Communication](#using-ssl-communication)
  * [Working with XA Transactions](#working-with-xa-transactions)
  * [Working with Solace Disaster Recovery](#working-with-solace-disaster-recovery)
  * [Using an external JNDI store for Solace JNDI lookups](#using-an-external-jndi-store-for-solace-jndi-lookups)

### Related Documentation

* [Solace Developer Portal]({{ site.links-dev-portal }}){:target="_top"}
* [Solace Messaging API for JMS]({{ site.links-docs-jms }}){:target="_top"}
* [Solace JMS API Online Reference Documentation]({{ site.links-docs-jms-api }}){:target="_top"}
* [Solace Feature Guide]({{ site.links-docs-features }}){:target="_top"}
* [Solace PubSub+ Message Broker Configuration]({{ site.links-docs-router-config }}){:target="_top"}
* [Solace Command Line Interface Reference]({{ site.links-docs-cli }}){:target="_top"}
* [WebSphere Application Server Information Library]({{ site.links-was-reference }}){:target="_top"}
* [Java Connector Architecture v1.5](https://jcp.org/en/jsr/detail?id=112 ){:target="_blank"}

{% include_relative assets/solaceMessaging.md %}

## Integrating with WebSphere Application Server

Solace provides a JCA compliant resource adapter for integrating Java enterprise applications with the Solace PubSub+ message broker.  There are several options for deploying a Resource Adapter for use by Java enterprise applications including embedded and stand-alone deployment.  Solace provides a Resource Adapter Archive (RAR) file for stand-alone deployment.

In order to illustrate WebSphere Application Server integration, the following sections will highlight the required WebSphere configuration changes, and provide sample code for sending and receiving messages using Enterprise Java Beans. 

This EJB sample consists of two enterprise beans, a Message Driven Bean and a Session Bean.  The MDB is configured to receive a message on a `requests` Queue.  When the MDB receives a message it then calls a method of the Session Bean to send a reply message to a `replies` Queue.  The EJB sample requires configuration of various J2C entities in WebSphere to support usage of the Solace JCA compliant resource adapter.

The following steps are required to accomplish the above goals of sending and receiving messages using the Solace PubSub+ message broker. 

### Description of Resources Required

The Solace JCA 1.5 resource adapter is provided as a standalone RAR file and is versioned together with a specific release of the Solace JMS API.  The JMS API libraries are bundled inside a single resource adapter RAR file for deployment to the WebSphere application server.

* Resource: Solace JCA 1.5 resource adapter stand-alone RAR file (sol-jms-ra-%RELEASE%.rar)
* Download Options: 
  * [Solace Developer Portal]({{ site.links-downloads }}){:target="_top"} - Under JMS API
  * [Solace Customer SFTP]({{ site.links-jms-cust-rar-download }}){:target="_top"} - Path: `%VERSION%/Topic_Routing/APIs/JMS/Current/%RELEASE%`

This integration guide will demonstrate the creation of Solace resources and the configuration of the WebSphere Application Server's managed resources. The next section outlines the resources that are created and used.


#### Solace Resource Naming Convention

To illustrate this integration example, all named resources created on the Solace PubSub+ message broker will have the following prefixes:

| **Resource** | **Prefix** |
| Non-JNDI resource | solace_%RESOURCE_NAME% |
| JNDI names | JNDI/Sol/%RESOURCE_NAME% |

#### Solace Resources

The following Solace PubSub+ message broker resources are required for the integration sample in this document.

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

To illustrate this integration example, all named resources created in WebSphere application server will have the following prefixes:

| **Resource** | **Prefix** |
| Non-JNDI resource | j2c_%RESOURCE_NAME% |
| JNDI names | JNDI/J2C/%RESOURCE_NAME% |

#### Application Server Resources

The following WebSphere application server resources are required for the integration example in this document.

| **Resource** | **JNDI Name** | **JNDI Value** | **Description** |
| Resource Adapter | N/A | N/A | The Solace JMS Resource Adapter packaged as a Standalone RAR package (Implements a JCA compliant Resource Adapter) |
| J2C connection factory | j2c_cf | JNDI/J2C/CF | A J2C entity used to access a Solace javax.jms.ConnectionFactory (For Outbound messaging) |
| J2C activation specification | j2c_as | JNDI/J2C/AS | A J2C entity used to initialize a Message Driven Bean and to bind it to a Solace JMS destination using the Solace implementation of javax.jms.MessageListener (For Inbound messaging) |
| J2C administered object | j2c_request_queue | JNDI/J2C/Q/requests | A J2C entity used to perform a JNDI lookup of a javax.jms.Queue on the message broker |
| J2C administered object | j2c_reply_queue | JNDI/J2C/Q/replies | A J2C entity used to perform a JNDI lookup of a javax.jms.Queue on the message broker |

### Solace JMS provider Configuration

The following entities on the Solace PubSub+ message broker need to be configured at a minimum to enable JMS to send and receive messages within the WebSphere Application Server.

* A Message VPN, or virtual message broker, to scope the integration on the message broker
* Client connectivity configurations like usernames and profiles
* Guaranteed messaging endpoints for receiving and sending messages.
* Appropriate JNDI mappings enabling JMS clients to connect to the message broker configuration.

{% include_relative assets/solaceConfig.md %}

{% include_relative assets/solaceVpn.md content="solace_VPN" %}

#### Configuring Client Usernames & Profiles

This section outlines how to update the default client-profile and how to create a client username for connecting to the message broker For the client-profile, it is important to enable guaranteed messaging for JMS messaging and transacted sessions for the XA-transactions capable Solace JCA Resource Adapter.
The chosen client username of "solace_user" will be required by the WebSphere Application Server when connecting to the message broker

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

This integration guide shows receiving messages and sending reply messages within the WebSphere Application Server using two separate JMS Queues. For illustration purposes, these queues are chosen to be exclusive queues with a message spool quota of 2GB matching quota associated with the message VPN. The queue names chosen are "solace_requests" and "solace_replies".

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

#### Setting up Solace JNDI References

To enable the JMS clients to connect and look up the queue destination required by WebSphere Application Server, there are three JNDI objects required on the message broker:

* A connection factory: JNDI/Sol/CF
  * Note: Ensure `direct-transport` is disabled for JMS persistent messaging.
* A queue destination: JNDI/Sol/Q/requests
* A queue destination: JNDI/Sol/Q/replies

They are configured as follows:

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

## Deploying Solace JMS Resource Adapter

Solace provides a JCA compliant Resource Adapter that can be deployed to the WebSphere application server allowing Enterprise Java Beans to connect to Solace through a standard JCA interface.  This integration guide outlines the steps to deploy the resource adapter which is provided by Solace as a packaged stand-alone RAR file. This is the recommended way of integrating Solace with WebSphere as it provides support for XA transactions.

The following Java system property must be configured in the application server JVM properties:

```
-DpasswordDecoderClassName=com.ibm.ISecurityUtilityImpl.PasswordUtil
-DpasswordDecoderMethodName=passwordDecode
```

**Note**: if using WebSphere 7 configure:

```
  -DpasswordDecoderClassName=com.ibm.ws.security.util.PasswordDecoder
  -DpasswordDecoderMethodName=decodePassword
```

The above properties allow the resource adapter to decrypt authentication credentials encrypted by WebSphere before sending them to the Solace message broker.

Steps to configure application server JVM properties:

1.	Log into the WebSphere Application Server administrative console.

1. Click on the 'servers > Server Types > WebSphere application servers' link in the navigation pane

1. In the Application Servers page, click on your specific application server link

1. Expand the list 'Java and Process Management' under section '- Server Infrastructure' and select the 'Process definition' link

1. Select the 'Java Virtual Machine' link under the section '– Additional Properties'

1. Specify the above Java system properties in the 'Generic JVM arguments' field

1. Click the 'save' link to commit the changes to the application server

1. Restart the application server

![]({{ site.baseurl }}/images/WebSphere/config-jvm-1.png)

<br/>

### Adding the Solace Resource Adapter as a Resource

The following steps will make the Solace Resource Adapter available as a resource to all enterprise applications (Refer to the above section [Description of Resources Required] for the file location of the Solace JCA 1.5 Resource Adapter RAR file).

Steps to deploy the Solace JMS Resource Adapter:

1.	Log into the WebSphere Application Server administrative console.

1.	Click on the 'Resources > Resource Adapters > Resource Adapters' link in the navigation pane

1.	In the Resource adapters page, click on the 'Install RAR' button

1.	In the Install RAR File page, select the file path to the Solace JMS Resource Adapter RAR (.rar) file, then click on the 'Next' button

1.	Click the 'OK' button

1.	Click the 'save' link to commit the changes to the application server

![]({{ site.baseurl }}/images/websphere/add-resource-adapter.png)


### Configuring the Solace Resource Adapter properties

Connecting to the message broker through the Solace JMS Resource Adapter requires configuration of additional resources in WebSphere.  Two key pieces of information are required including connectivity information to the message broker and client authentication data.

The above information is specified across one or more J2C entities depending on your application’s JMS message flow (Inbound, Outbound, or both).  Configuration of a J2C connection factory is required for outbound message flow, while configuration of a J2C activation specification is required for inbound message flow.

The Solace Resource Adapter includes several custom properties for specifying connectivity and authentication details to the Solace message broker.  Setting these properties at the Resource Adapter level makes the information available to all child J2C entities like J2C connection factory, J2C activation specification and J2C administered objects.  The properties can also be overridden at the J2C entity level allowing connectivity to multiple message brokers.

Please refer to [WAS-REF]({{ site.links-was-reference }}){:target="_top"} and [JCA-1.5](https://jcp.org/en/jsr/detail?id=112 ){:target="_blank"}
 for more details on configuring general JEE authentication options. The [Authentication](#authentication) section below discusses configuration of Solace specific authentication in more detail. 

Steps to configure the Solace JMS Resource Adapter:

1. Log into the WebSphere Application Server administrative console.

1. Click on the 'Resources > Resource Adapters > Resource adapters' link in the navigation pane.

1. Edit the custom properties for the Solace JMS Resource Adapter:

  * Click on the 'Custom properties' link under the 'Additional properties' section.

![]({{ site.baseurl }}/images/WebSphere/config-ra-1.png)


  * Update the value for the custom properties  'ConnectionURL', 'UserName', 'Password', and 'MessageVPN':

    i. Click on the 'ConnectionURL' property and specify the value `tcp://IP:Port` (Update the value '__IP:Port__' with the actual message broker message-backbone VRF IP ).

    ii. Click on the 'Apply' button and then the 'Save' link to commit the changes to the application server.

    iii. Click on the 'UserName' property and specify the value corresponding to the Solace username (use the value `solace_user` for this example).

    iv. Click on the 'Apply' button and then the 'Save' link to commit the changes to the application server.

    v. Click on the 'Password' property and specify the value for the Solace username, use the value 
    `solace_password` (**Note**: in section Configuring Client Usernames & Profiles we specified a Solace authentication type of 'none', so the password here will be ignored but WebSphere mandates a non-empty password).

    vi. Click on the 'MessageVPN' property and specify the value corresponding to the Solace message VPN (use the value `solace_VPN` for this example).

    vii. Click on the 'Apply' button and then the 'Save' link to commit the changes to the application server. 

![]({{ site.baseurl }}/images/WebSphere/config-ra-2.png)

The following table summarizes the values used for the resource adapter's bean properties.

| **Name** | **Value** | **Description** |
| ConnectionURL | tcp://IP:Port | The connection URL to the message broker of the form: `tcp://IP:Port` (Update the value `IP:Port` with the actual message broker message-backbone VRF IP) |
| MessageVPN | solace_VPN | A Message VPN, or virtual message broker, to scope the integration on the message broker. |
| UserName | solace_user | The client username credentials on the message broker |
| Password |  | Optional password of the Client Username on the message broker |
| ExtendedProps |  | Comma-seperated list for advanced control of the connection.  For this example, leave empty.  Supported values are shown below. |

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

### Configuring Connection Factories

A client must use a J2C Connection Factory to create a JMS connection to the Solace PubSub+ message broker. A JMS connection establishes a data channel between the message broker and the JMS application. The Connection Factory provides a number of configuration parameters to customize the connection between the JMS client and the message broker.

Steps to create a J2C Connection Factory:

1. Log into the WebSphere Application Server administrative console.

1. Click on the ‘Resources > Resource adapters > J2C connection factories’ link in the navigation pane.

1. In the J2C connection factories page, click on the ‘New’ button.

1. Specify the Provider as ‘Solace JMS RA’ in the drop-down menu.

1. Specify a unique Name for this J2C Connection Factory (Name it `j2c_cf` for this example).

1. Specify the JNDI name that will be used by clients to lookup this J2C Connection Factory (Name it `JNDI/J2C/CF` for this example).

1. Specify the type of connection factory desired using the drop-down menu ‘Connection factory interface’ (use the value `javax.jmx.ConnectionFactory` for this example).

1. Click the ‘OK’ button.

1. Click the ‘Save’ link to commit the changes to the application server.

1. Edit the J2C Connection Factory custom properties:

  * Click on the ‘j2c_cf’ link in the J2C connection factories page, then click on the ‘Custom properties’ link under the ‘Additional properties’ section.

![]({{ site.baseurl }}/images/WebSphere/config-cf-1.png)

  * Specify value for ‘ConnectionFactoryJndiName’.
  
    i.	Click on the ‘ConnectionFactoryJndiName’ property and specify the value `JNDI/Sol/CF` in the Value field. (**Note**: this is the value configured on the message broker in section Setting up Solace JNDI References).
    
    ii.	Click on the ‘Apply’ button and then the ‘save’ link to commit your changes to the Master configuration. 

![]({{ site.baseurl }}/images/WebSphere/config-cf-2.png)

The following table summarizes the values used for the J2C connection factory custom properties.

| **Name** | **Value** | **Description** |
| ConnectionFactoryJndiName | JNDI/Sol/CF | The JNDI name of the JMS connection factory as configured on the message broker. |


### Configuring Connection Pooling for a connection factory

The Connection Pools tab allows the configuration of the amount of concurrent connections that are run between the WebSphere server and the message broker. Those connections are shared between the requests coming from the applications, therefore the resources can be used more efficiently (limited number of connections) and optimized for performance (pre-initialized connections).

The amounts to be set vary, depending on the demand of the WebSphere application(s). Capacity planning and load testing should identify the appropriate values. E.g. if only every 3rd request is using messaging, the pool size can be reduced approximately by that factor from the maximum amount of concurrent requests.

![]({{ site.baseurl }}/images/WebSphere/con-pooling.png)

The most relevant values are:

| Maximum connections | Maximum number of connection created for this pool. |
| Minimum connections | Minimum number of connections maintained in the pool. |

Save and return to the resource adapter configuration page.

### Configuring Activation Specifications

To receive messages from the Solace JMS provider, the client is bound to a J2C Activation Specification which specifies the JMS destination from which to consume messages as well as the authentication details used to connect to the message broker. 

Steps to create a J2C Activation Specification:

1. Log into the WebSphere Application Server administrative console.

1. Click on the ‘Resources > Resource Adapters > J2C activation specifications’ link in the navigation pane.

1. In the J2C activation specifications page, click on the ‘New’ button.

1. Specify a value for the Name field (use the value `j2c_as` for this example).

1. Specify a value for the JNDI name field (use the value `JNDI/J2C/AS` for this example).

1. Specify a value for the Message listener type using the drop-down menu (use the value `javax.jms.MessageListener supported by com.solacesystems.jms.ra.inbound.ActivationSpec`).

1. Click the ‘OK’ button.

1. Click the ‘Save’ link to commit the changes to the application server.

1. Edit the J2C activation specification custom properties:

  * Click on the ‘j2c_as’ link in the J2C activation specifications page then click on the ‘J2C administered objects custom properties’ link under the ‘Additional properties’ section.

![]({{ site.baseurl }}/images/WebSphere/config-as-1.png)
  
  * Specify values for the properties connectionFactoryJndiName, destination, and destinationType:
    
    i.	Click on the ‘connectionFactoryJndiName’ property and specify the value ‘JNDI/Sol/CF’ in the Value field. (**Note**: this is the value configured on the message broker in Section 3.2.4 Setting up Solace JNDI References).
    
    ii.	Click on the ‘Apply’ button and then the ‘save’ link to commit your changes to the Master configuration.
    
    iii.	Click on the ‘destination’ property and specify the value ‘JNDI/Sol/Q/requests’ in the Value field. (**Note**: this is the value configured on the message broker in Section 3.2.4 Setting up Solace JNDI References).
    
    iv.	Click on the ‘Apply’ button and then the ‘save’ link to commit your changes to the Master configuration.
    
    v.	Click on the ‘destinationType’ property and specify the value ‘javax.jms.Queue’ in the Value field.
    
    vi.	Click on the ‘Apply’ button and then the ‘Save’ link to commit your changes to the Master configuration.

![]({{ site.baseurl }}/images/WebSphere/config-as-2.png)

The following table summarizes the values used for the J2C activation specification custom properties.

| **Name** | **Value** | **Description** |
| connectionFactoryJndiName | JNDI/Sol/CF | The JNDI name of the JMS connection factory as configured on the message broker. |
| destination | JNDI/Sol/Q/requests | The JNDI name of the JMS destination as configured on the message broker. |
| destinationType | javax.jms.Queue | The JMS class name for the desired destination type. |

### Configuring Administered Objects

A J2C Administered Object is required for a client to sends outbound messages to a JMS destination.

Steps to create a J2C administered object (of type Queue)

1. Log into the WebSphere Application Server administrative console.

1. Click on the ‘Resources > Resource Adapters > J2C administered objects’ link in the navigation pane.

1. In the J2C connection factories page, click on the ‘New’ button.

1. Specify the Provider as ‘Solace JMS RA’ in the drop-down menu.

1. Specify a value for the Name field of this end point (use the value `j2c_reply_queue` for this example).

1. Specify a value for the JNDI Name of this end point (use the value `JNDI/J2C/Q/replies` for this example).

1. Specify the Administered object class for this end point using the drop-down menu (use the value `com.solacesystems.jms.ra.outbound.QueueProxy implements javax.jms.Queue` for this example).

1. Click the ‘OK’ button.

1. Click the ‘Save’ link to commit the changes to the application server.

1. Edit the J2C administered object’s custom properties:

  * Click on the ‘j2c_reply_queue’ link in the J2C administered objects page then click on the ‘J2C administered objects custom properties’ link under the ‘Additional properties’ section.

![]({{ site.baseurl }}/images/WebSphere/config-ao-1.png)

  * Specify a value for the property ‘Destination’:
  
    i.	Click on the ‘Destination’ property and specify the value `JNDI/Sol/Q/replies` in the Value field. (**Note**: this is the value configured on the message broker in section Setting up Solace JNDI References).
    
    ii.	Click on the ‘Apply’ button.
    
    iii.	Click the ‘Save’ link to commit the changes to the application server.

![]({{ site.baseurl }}/images/WebSphere/config-ao-2.png)


The following table summarizes the values used for the J2C administered object custom properties:

| **Name** | **Value** | **Description** |
| Destination | JNDI/Sol/Q/replies | The JNDI name of the JMS destination as configured on the message broker. |

### Redeploy the resource adapter with the new settings

Restart the WebSphere Application Server for these changes to take effect.

## Sample Application Code

Source code for an Enterprise Java Beans (EJB) application for WebSphere, implementing waiting for receiving and then sending a message, has been provided in the GitHub repo of this guide.

There are three variants:

* [EJBSample-WAS/ejbModule]({{ site.repository }}/blob/master/src/websphere/EJBSample-WAS/ejbModule){:target="_blank"} - used in this basic application sample.
* EJBSample-WAS-XA-BMT/ejbModule - used in the [Working with XA Transactions](#working-with-xa-transactions) sample.
* EJBSample-WAS-XA-CMT/ejbModule - used in the [Working with XA Transactions](#working-with-xa-transactions) sample.

The structure of all variants is the same:

* Java source files under `ejbModule/com/solace/sample/`
  * ConsumerMDB.java - implements a message-driven EJB bean, triggered on receiving a message from the Solace message broker
  * ProducerSB.java - implements `sendMessage()` in a session EJB bean, used to send a message to the Solace message broker
  * Producer.java - "Producer"'s remote EJB interface to call `sendMessage()`
  * ProducerLocal.java - "Producer"'s local EJB interface to call `sendMessage()`

* EJB XML descriptors under `ejbModule/META-INF/`
  * ejb-jar.xml - deployment descriptor providing required EJB configuration information
  * ibm-ejb-jar-bnd.xml - descriptor providing WebSphere application binding information

### Receiving messages from Solace – Sample Code

The sample code below shows the implementation of a message-driven bean ("ConsumerMDB") which listens for JMS messages to arrive on the configured Solace connection factory and destination (`JNDI/Sol/CF` and `JNDI/Sol/Q/requests` respectively - as configured in the J2C activation specification).  Upon receiving a message, the MDB calls the method sendMessage() of the "ProducerSB" session bean which in turn sends a reply message to a ‘reply’ Queue destination.

```java
@TransactionManagement(value = TransactionManagementType.BEAN)
@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)

@MessageDriven
public class ConsumerMDB implements MessageListener {

    @EJB(beanName = "ProducerSB", beanInterface = Producer.class)
    Producer sb;

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
    }
}
```

The full source code for this example is available in the following source:

 * [ConsumerMDB.java]({{ site.repository }}/blob/master/src/websphere/EJBSample-WAS/ejbModule/com/solace/sample/ConsumerMDB.java){:target="_blank"}

### Sending Messages to Solace – Sample code

The sample code below shows the implementation of a session bean ("ProducerSB") that implements a method sendMessage() which sends a JMS message to the Queue destination configured above.  The sendMessage() method is called by the "ConsumerMDB" bean outlined in the previous section.

This example uses Java resource injection for the resources ‘myCF’ and ‘myReplyQueue’ which are mapped to their respective J2C entities using an application binding file (see example application bindings file following the code example below).

```java
@Stateless(name = "ProducerSB")
@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)

public class ProducerSB implements Producer, ProducerLocal {
    @Resource(name = "myCF")
    ConnectionFactory myCF;

    @Resource(name = "myReplyQueue")
    Queue myReplyQueue;

    public ProducerSB() {
    }

    @TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public void sendMessage() throws JMSException {

    System.out.println("Sending reply message");
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

The code sample illustrated in this guide uses the following EJB application bindings file:

```
<!-- ProducerSB (Session Bean) resources binding -->
<session name="ProducerSB">
  <resource-ref name="myCF" binding-name="JNDI/J2C/CF" />
  <resource-ref name="myReplyQueue" binding-name="JNDI/J2C/Q/replies" />
</session>

<!-- ConsumerMDB (Message Driven Bean) activation specification binding -->
<message-driven name="ConsumerMDB">
  <jca-adapter activation-spec-binding-name="JNDI/J2C/AS" />
</message-driven>
```

The full source code for this example is available in the following sources:

 * [ProducerSB.java]({{ site.repository }}/blob/master/src/websphere/EJBSample-WAS/ejbModule/com/solace/sample/ProducerSB.java){:target="_blank"}
 * [ibm-ejb-jar-bnd.xml]({{ site.repository }}/blob/master/src/websphere/EJBSample-WAS/ejbModule/META-INF/ibm-ejb-jar-bnd.xml){:target="_blank"}

### Building the samples

Instructions are provided for "Eclipse IDE for Java EE Developers" with "WebSphere Application Server Developer Tools for Eclipse" installed from Eclipse Marketplace. Adjust the steps accordingly if your environment differs.

Follow these steps to create and build your project:

1. Clone this project from GitHub
```
git clone https://github.com/SolaceLabs/solace-integration-guides.git
cd solace-integration-guides/src/websphere/EJBSample-WAS/ejbModule/
```
1. Create a new "EJB project" in Eclipse, set the target runtime to WebSphere Application Server. Optionally check the "Add your project to an EAR" to create an Enterprise Archive instead of an EJB JAR.

1. Replace the new project `ejbModule` directory contents (created empty) with the contents of the `ejbModule` directory of this repo, then refresh your project in the IDE.

1. Export your project to an EJB JAR file or alternatively, if you have a related EAR project created then export from there to an EAR file.

You have now built the sample application as a deployable JAR or EAR archive. Take note of the file and directory location.

### Deploying the sample application

Steps to deploy the sample application:

1. Log into the WebSphere Application Server administrative console.

1. Click on the ‘Applications > Application Types > WebSphere enterprise applications’ link in the navigation pane.

1. In the Enterprise Applications page, click on the ‘Install’ button.

1. Specify the location of the JAR or EAR archive, then click ‘Next’.

1. Click through ‘Next’ on the following screens, then ‘Finish’.

1. Click the ‘Save’ link to commit the changes to the application server.

1. Select and click on ‘Start’ to start your application.

You have now deployed the sample application and it is ready to receive messages from the `solace_requests` queue on the message broker.

### Testing the sample application

To send a test message you can use the **queueProducerJNDI** sample application from the [Obtaining JMS objects using JNDI](https://dev.solace.com/samples/solace-samples-jms/using-jndi/ ) tutorial. Ensure to adjust in the source code the "CONNECTION_FACTORY_JNDI_NAME" and "QUEUE_JNDI_NAME" to `JNDI/Sol/CF` and `JNDI/Sol/Q/requests` respectively, as used in this tutorial.

Once a message has been sent to the `solace_requests` queue it will be delivered to the enterprise application, which will consume it from there and send a new message to the `solace_replies` queue.

You can check the messages in the `solace_replies` queue using the using [Solace PubSub+ Manager]({{ site.links-docs-webadmin }}){:target="_top"}, Solace's browser-based administration console.

You can also check how the message has been processed in WebSphere logs as described in section "Debugging Tips for Solace JMS API Integration".
 
## Performance Considerations

The Solace JMS Resource Adapter relies on the WebSphere Application Server for managing the pool of JMS connections.  Tuning performance for outbound messaging can in part be accomplished by balancing the maximum number of pooled connections available against the number of peak concurrent outbound messaging clients.

For inbound messaging there are different levers that can be tuned for maximum performance in a given environment.  The ‘batchSize’ custom property of the Solace J2C activation specification defines the maximum number of messages retrieved at a time from a JMS destination for delivery to a server session.  The server session then routes those messages to respective Message Driven Beans.  In addition, the ‘maxPoolSize’ custom property of the Solace J2C AS defines the maximum number of pooled JMS sessions that can be allocated to MDB threads.  Therefore to fine tune performance for inbound messaging, the ‘batchSize’ and ‘maxPoolSize’ must be balanced to the rate of incoming messages.

Another consideration is the overhead of performing JNDI lookups to the message broker.  WebSphere implements JNDI caching by default.  Resources referenced by a Message Driven Bean through resource injection will trigger an initial JNDI lookup and subsequently use the cached information whenever the MDB instance is reused from the MDB pool. Similarly, Session beans that perform JNDI lookups through a JNDI Context will have that information cached in association with that context.  Whenever the Session bean instance is reused from the Session bean pool, any lookups using the same JNDI Context will utilize the JNDI cached information.

**Note**: in order to use the JNDI caching mechanisms within WebSphere you must use JMS through a JCA resource adapter and reference JMS end points in your code through JEE resource injection.
Please refer to [WAS-REF]({{ site.links-was-reference }}){:target="_top"} for details on modifying the default behavior of JNDI caching.

In general, refer to [WAS-REF]({{ site.links-was-reference }}){:target="_top"} for details on tuning WebSphere for messaging performance:

*	WebSphere Application Server > Tuning performance > Messaging resources > Tuning messaging


## Working with Solace High Availability (HA)

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

In addition to configuring the above properties for connection factories, care should be taken to configure connection properties for performing JNDI lookups to the message broker.  These settings can be configured in the WebSphere application server globally by setting them at the Solace Resource Adapter level or within individual J2C entities.  

To configure JNDI connection properties for JNDI lookups, set the corresponding Solace JMS property values (as a semi-colon separated list of name=value pairs) through the 'ExtendedProps' custom property of the Solace Resource Adapter or J2C administered objects.

* Solace_JMS_JNDI_ConnectRetries = 1
* Solace_JMS_JNDI_ConnectRetriesPerHost = 5
* Solace_JMS_JNDI_ConnectTimeout = 30000 (milliseconds)
* Solace_JMS_JNDI_ReadTimeout = 10000 (milliseconds)
* Solace_JMS_JNDI_ReconnectRetries = 20 
* Solace_JMS_JNDI_ReconnectRetryWait 3000 (milliseconds)

## Debugging Tips for Solace JMS API Integration

The key component for debugging integration issues with the Solace JMS API is to enable API logging. Enabling API logging from WebSphere Application Server is described below.

### How to enable Solace JMS API logging

You can enable different logging levels for the Solace Resource Adapter and Solace JMS API by enabling WebSphere Diagnostic Trace for specific Java Packages.  The configuration steps are outlined below together with the specific Java packages for which to enable diagnostic tracing.

**Note**: you can find the diagnostic trace logs in your JEE server logs directory (trace.log) along with other logs files such as SystemOut.log and SystemErr.log.

Steps to configure Diagnostic trace service for Solace JMS:

1. Log into the WebSphere Application Server administrative console.

2. Click on the ‘Servers > Server Types > WebSphere application servers’ link in the navigation pane.

3. Select your respective JEE server link In the Application servers page.

4. Click on the ‘Logging and tracing’ link under the Troubleshooting section.

5. Click on the ‘Diagnostic Trace link’.

6. Click on the ‘Change log detail levels’ link under the ‘Additional Properties’ section.

    * Expand the ‘Components and Groups’ node, then the ‘’[All Components]’ node to reveal all deployed Java packages.
    
    * Select one of the following Solace Java packages to enable a specific logging level:
    
      i.	com.solacesystems.common.*
      
      ii.	com.solacesystems.jcsmp.*
      
      iii.	com.solacesystems.jms.*
      
        Note: the implementation of the Solace JMS Resource Adapter is implemented by package com.solacesystems.jms.ra
      
    * With one of the above nodes selected, select a logging level from the ‘Message and Trace Levels’ menu.
    
    * Click the ‘OK’ button.
    
    * Click the ‘Save’ link to commit the changes to the application server.
  
7. Optional, modify the ‘File Name’ value to specify an alternate location for the Diagnostic trace log file.

8. Click the ‘OK’ button.

9. Click the ‘Save’ link to commit the changes to the application server.

10. **Note**: the application server must be restarted for the above changes to take effect.

### Enabling JConsole Support for Mbean Monitoring

You can use JMX to monitor MDBs that use the Solace resource adapter by registering the MDB with the Solace resource adapter – Management Bean (MBean).  The JConsole tool can then be used to monitor the Solace server session pool.

To register a Message Driven Bean with the Solace MBean you must give the MDB a unique name through the MDB’s associated J2C activation specification – custom property ‘endpointName’.

To enable viewing of the Solace resource adapter MBean and other JVM performance metrics in JConsole, do the following:

1. Log into the WebSphere Application Server administrative console

2. Click on Servers > Server Types > WebSphere application servers > [your server]

3. In the Application Servers page, for your application server, under section ‘Server Infrastructure’, click on Java and Process Management > Process Definition > Java Virtual Machine

4. Enter the following space delimited name/value pairs under the ‘Generic JVM arguments’ field: 

    * `-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=<port_number> -Djavax.management.builder.initial= -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false`
    
5. Launch jconsole.exe (from <JDK_Install_Dir>/bin directory)

6. From the JConsole dialog, click Remote Process option and enter (Where `<port>` is the port number specified above, and `<host>` is the host on which your JEE application server is running):

    * `service:jmx:rmi:///jndi/rmi://<host>:<port>/jmxrmi`
    
7. Once connected, you can click on the MBeans tab to view the MBean under com.solacesystems.jms.ra.

## Advanced Topics

### Authentication

The integration example illustrated in this guide uses the authentication information specified in the custom properties of the Solace Resource Adapter.  These authentication properties are used whenever Application Managed authentication is specified for a JCA resource.  No matter the authentication mode (Application-Managed or Container-Managed) specified for a resource, the Solace 'MessageVPN' information for a connection is always retrieved from the Solace Resource Adapter configured properties, or the configured properties of the respective J2C resource.

WebSphere supports configuration of Container-Managed authentication for J2C resources.  The administrator of an EJB application can configure message broker sign-on credentials using a J2C authentication alias that is assigned to either a J2C activation specification or J2C connection factory.  Solace JCA resource adapter supports authentication using the 'DefaultPrincipalMapping' mapping configuration alias. Refer to [WAS-REF]({{ site.links-was-reference }}){:target="_top"} for more details on configuring J2C authentication data aliases.

The message broker supports a variety of client authentications schemes as described in the Solace documentation [Client Authentication and Authorization]({{ site.links-docs-client-authenticate-authorize }}){:target="_top"}.  The Solace JCA resource adapter supports a subset of these schemes including `Basic` authentication and 'SSL Client Certificate' authentication.  The default authentication scheme used by the Solace JMS Resource Adapter is AUTHENTICATION_SCHEME_BASIC.

The value of the Solace Resource Adapter custom property 'extendedProps' is used to specify an alternate authentication scheme such as 'AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE'. The value of the custom property 'extendedProps' consists of a semi-colon separated list of Solace JMS property / value pairs (SOLACE_PROPERTY=value).  You can specify the required properties for an alternate authentication scheme using this technique.  Refer to the [Solace JMS API Online Reference Documentation]({{ site.links-docs-jms-api }}){:target="_top"} for further details on the required JMS properties for configuring SSL client certificate authentication.

Although the authentication scheme AUTHENTICATION_SCHEME_BASIC is the default scheme, that scheme could also have been specified using the `extendedProps` custom property of the resource adapter.

* ExtendedProps - solace_JMS_Authentication_Scheme=AUTHENTICATION_SCHEME_BASIC

### Using SSL Communication

This section outlines how to update the message broker and WebSphere Application Server configuration to switch the client connection to using secure connections with the message broker. Forpurposes of illustration, this section uses a server certificate on the message broker and basic client authentication. It's possible to configure Solace JMS to use client certificates instead of basic authentication. This is done using configuration steps that are very similar to those outlined in this document. The [Solace Feature Guide]({{ site.links-docs-features }}){:target="_top"} and [Solace Messaging API for JMS]({{ site.links-docs-jms }}){:target="_top"} outline the extra configuration items required to switch from basic authentication to client certificates.

To change a WebSphere Application Server from using a plain text connection to a secure connection, the message broker configuration must first be updated, and the Solace JMS configuration within the WebSphere Application Server must be updated as outlined in the next sections.

#### Configuring the message broker

To enable secure connections to the message broker, the following configuration must be updated on the message broker.

* Server Certificate
* TLS/SSL Service Listen Port
* Enable TLS/SSL over SMF in the Message VPN

The following subsections outline how to configure these items.

##### Configure the Server Certificate

Before starting, here is some background information on the server certificate required by the message broker. This is from the [Solace-FP] section "Setting a Server Certificate"

```
  To enable the exchange of information through TLS/SSL-encrypted SMF service, 
  you must set the TLS/SSL server certificate file that the message broker
  is to use. This server certificate is presented to a client during the TLS/SSL
  handshakes. A server certificate used by an message broker must be an x509v3 
  certificate and it must include a private key. The server certificate and key
  use an RSA algorithm for private key generation, encryption and decryption, 
  and they both must be encoded with a Privacy Enhanced Mail (PEM) format.

  The single server certificate file set for the message broker can have a maximum
  chain depth of three (that is, the single certificate file can contain up to 
  three certificates in a chain that can be used for the certificate verification).
```

To configure the server certificate, first copy the server certificate to the message broker. For the purposes of this example, assume the server certificate file is named "mycert.pem".

```
# copy sftp://[<username>@]<ip-addr>/<remote-pathname>/mycert.pem /certs
<username>@<ip-addr>'s password:
#
```

Then set the server certificate for the message broker.

```
(config)# ssl server-certificate mycert.pem
(config)#
```

<br/>
##### Configure TLS/SSL Service Listen Port

By default, the message broker accepts secure messaging client connections on port 55443. If this port is acceptable then no further configuration is required and this section can be skipped. If a non-default port is desired, then follow the steps below. Note this configuration change will disrupt service to all clients of the message broker, and should therefore be performed during a maintenance window when client disconnection is acceptable. This example assumes that the new port should be 55403.

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

<br/>
##### Enable TLS/SSL within the Message VPN 

By default, within Solace Message VPNs both plain-text and SSL services are enabled. If the Message VPN defaults remain unchanged, then this section can be skipped. However, if within the current application VPN, this service has been disabled, then for secure communication to succeed it should be enabled. The steps below show how to enable SSL within the SMF service to allow secure client connections from the WebSphere Application Server. 

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

<br/>
#### Configuring the WebSphere Application Server

Secure connections to the Solace JMS provider require configuring SSL parameters on one or more J2C entities. While using the Solace Resource Adapter, these two parameters include changes to the Solace J2C custom properties 'ConnectionURL' and 'ExtendedProps'.  Note that the property values for 'ConnectionURL' and 'ExtendedProps' are inherited by J2C connection factory,,and J2C administered objects from their parent Resource Adapter.  Thus, unless you are connecting to multiple message brokers, a best practice is to configure values for 'ConnectionURL' and 'ExtendedProps' in the Solace Resource Adapter, otherwise the SSL related changes should be duplicated across custom properties for all of the J2C entities you want to secure.

The required SSL parameters include modifications to the URL scheme of 'ConnectionURL' (from `smf` to `smfs`), and setting additional SSL attributes through the custom property 'ExtendedProps'.  The following sections describe the required changes in more detail.

##### Updating the JMS provider URL (ConnectionURL)

In order to signal to the Solace JMS API that the connection should be a secure connection, the protocol must be updated in the URI scheme. The Solace JMS API has a URI format as follows:

```
<URI Scheme>://[username]:[password]@<IP address>[:port]
```

Recall from above, originally, the "ConnectionURL" was as follows:

```
tcp://___IP:PORT___
```

This specified a URI scheme of "smf" which is the plaint-text method of communicating with the message broker. This should be updated to "smfs" to switch to secure communication giving you the following configuration:

```
tcps://___IP:PORT___
```

<br/>
##### Specifying other SSL Related Configuration

The Solace JMS API must be able to validate the server certificate of the message broker in order to establish a secure connection. To do this, the following trust store parameters need to be provided.

First the Solace JMS API must be given a location of a trust store file so that it can verify the credentials of the message broker server certificate during connection establishment. This parameter takes a URL or Path to the trust store file.  

Specifying a value for the parameter 'solace_JMS_SSL_TrustStore' is accomplished by modifying the Solace J2C custom property 'ExtendedProps'. The value for the property is comprised of a semi-colon separated list of Solace JMS parameters.

```
Solace_JMS_SSL_TrustStore=___Path_or_URL___
```

A trust store password may also be specified. This password allows the Solace JMS API to validate the integrity of the contents of the trust store. This is done through the Solace JMS parameter 'solace_JMS_SSL_TrustStorePassword'.

```
Solace_JMS_SSL_TrustStorePassword=___Password___
```

There are multiple formats for the trust store file. By default Solace JMS assumes a format of Java Key Store (JKS). So if the trust store file follows the JKS format then this parameter may be omitted. Solace JMS supports two formats for the trust store: "jks" for Java Key Store or "pkcs12". Setting the trust store format is done through the parameter 'solace_JMS_SSL_TrustStoreFormat':

```
Solace_JMS_SSL_TrustStoreFormat=jks
```

In a similar fashion, the authentication scheme used to connect to Solace may be specified using the parameter 'solace_JMS_Authentication_Scheme':

* AUTHENTICATION_SCHEME_BASIC 
* AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE

The integration examples in this guide use basic authentication (the default authentication scheme):

```
Solace_JMS_Authentication_Scheme=AUTHENTICATION_SCHEME_BASIC
```

Steps to update the ‘extendedProps’ custom property of J2C Connection Factory:

1. Log into the WebSphere Application Server administrative console.

1. Click on the ‘Resources > Resource adapters > J2C connection factories’ link in the navigation pane

1. Edit the J2C Connection Factory:

    * Click on the desired connection factory link in the J2C connection factories page
    
    * Update the value for the custom property ‘extendedProps’:
    
      i.	Click on the ‘Custom properties’ link under the ‘Additional properties’ section
      
      ii.	Click on the ‘extendedProps’ property and specify the value (Update the values ‘__Path_or_URL__’ and ‘__Password__’ accordingly)
      
      Note: following must be in one line and spaces between the JMS property name and preceding semi-colon are not allowed

      `Solace_JMS_Authentication_Scheme=AUTHENTICATION_SCHEME_BASIC;`
      `Solace_JMS_SSL_TrustStore=___Path_or_URL___;`
      `Solace_JMS_SSL_TrustStorePassword=___Password___;`
      `Solace_JMS_SSL_TrustStoreFormat=jks`
      
      iii.	Click on the ‘Apply’ button.
      
      iv.	Click the ‘Save’ link to commit the changes to the application server


### Working with XA Transactions

This section demonstrates how to configure the message broker to support XA transaction processing capabilities of the Solace JCA Resource Adapter.  Also, code examples are provided showing JMS message consumption and production over XA transactions using both Container-Managed-Transactions (CMT) and Bean-Managed-Transaction (BMT) configuration.

XA transactions are supported in the general-availability release of SolOS version 7.1 and above.  The Solace JCA Resource Adapter provides XA Transaction support in version 7.2 and above.

In addition to the standard XA Recovery functionality provided through the Solace JCA Resource Adapter, SolOS version 7.1 provides XA transaction administration facilities in the event that customers must perform manual failure recovery. Refer to the document [Solace JMS API Online Reference Documentation]({{ site.links-docs-jms-api }}){:target="_top"} for full details on administering and configuring XA Transaction support on the message broker.

#### Enabling XA Support for JMS Connection Factories

To enable XA transaction support for specific JMS connection factories the customer must configure XA support for the respective JNDI connection factory on the message broker using the [Solace PubSub+ Manager]({{ site.links-docs-webadmin }}){:target="_top"} admin console or the CLI as follows:  

```
(config)# jndi message-vpn solace_VPN
(config-jndi)# connection-factory JNDI/Sol/CF
(config-jndi-connection-factory)# property-list messaging-properties
(config-jndi-connection-factory-pl)# property xa true
(config-jndi-connection-factory-pl)# exit
(config-jndi-connection-factory)# exit
(config-jndi)#
```

#### XA Transactions – Sample Code

The following examples demonstrate how to receive and send messages using XA transactions.  Examples are given for both BMT and CMT in GitHub:

* [EJBSample-WAS-XA-BMT/ejbModule]({{ site.repository }}/blob/master/src/websphere/EJBSample-WAS-XA-BMT/ejbModule/){:target="_blank"}
* [EJBSample-WAS-XA-CMT/ejbModule]({{ site.repository }}/blob/master/src/websphere/EJBSample-WAS-XA-CMT/ejbModule/){:target="_blank"}

For building and deployment instructions refer to the [Sample Application Code](#building-the-samples) section.

##### Receiving messages from Solace over XA transaction – CMT Sample Code

The following code is similar to the basic "ConsumerMDB" example, but specifies Container-Managed XA Transaction support for inbound messages - see in the `@TransactionManagement` annotation.  In this example, the Message-Driven-Bean (MDB) - 'XAConsumerMDB' is configured such that the EJB container will provision and start an XA transaction prior to calling the onMessage() method and finalize or rollback the transaction when onMessage() exits (Rollback typically occurs when an unchecked exception is caught by the Container).

```java
@TransactionManagement(value = TransactionManagementType.CONTAINER)
@TransactionAttribute(value = TransactionAttributeType.REQUIRED)

@MessageDriven
public class XAConsumerMDB implements MessageListener {

    @EJB(beanName = "XAProducerSB", beanInterface = Producer.class)
    Producer sb;

    public XAConsumerMDB() {
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
    }
}
```

The full source code for this example is available here:

*    [XAConsumerMDB.java]({{ site.repository }}/blob/master/src/websphere/EJBSample-WAS-XA-CMT/ejbModule/com/solace/sample/XAConsumerMDB.java){:target="_blank"}

Note that it is important to limit the maximum number of XAConsumerMDB in the pool to ensure that the maximum per client concurrent transaction session count on the message broker is not exceeded. The maximum concurrent transacted sessioncount can be configured on the client-profile on the message broker

##### Sending Messages to Solace over XA Transaction – CMT Sample Code

The following code is similar to the "ProducerSB" EJB example from above, but configures Container-Managed XA Transaction support for outbound messaging.  In this example, the Session Bean 'XAProducerSB' method 'sendMessage()' requires that the caller have an existing XA Transaction context.  In this example, the 'sendMessage()' method is called from the MDB - 'XAConsumerMDB' in the above example where the EJB container has created an XA Transaction context for the inbound message.  When the method sendMessage() completes the EJB container will either finalize the XA transaction or perform a rollback operation.

```java
@Stateless(name = "XAProducerSB")
@TransactionManagement(value=TransactionManagementType.CONTAINER)
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

* [XAProducerSB.java]({{ site.repository }}/blob/master/src/websphere/EJBSample-WAS-XA-CMT/ejbModule/com/solace/sample/XAProducerSB.java){:target="_blank"}

##### Sending Messages to Solace over XA Transaction – BMT Sample Code

EJB code can use the UserTransaction interface (Bean-Managed) to provision and control the lifecycle of an XA transaction.  The EJB container won't provision XA transactions when the EJB class's 'TransactionManagement' type is designated as 'BEAN' managed.  In the following example, the session Bean 'XAProducerBMTSB' starts a new XA Transaction and performs an explicit 'commit()' operation after successfully sending the message.  If a runtime error is detected, then an explicit 'rollback()' operation is executed.  If the rollback operation fails, then the EJB code throws an EJBException() to allow the EJB container to handle the error.  

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
```
    
The full source code for this example is available here:

*    [XAProducerBMTSB.java]({{ site.repository }}/blob/master/src/websphere/EJBSample-WAS-XA-BMT/ejbModule/com/solace/sample/XAProducerBMTSB.java){:target="_blank"}

##### Enabling Local Transaction Support for EJBs

You can override the use of XA Transactions (the default transactional mode of the Solace JCA Resource Adapter) and force the use of Local Transactions for specific EJBs. **Note**: configuring Local Transactions for an EJB instructs the Transaction Manager in WebSphere to request the use of a Local Transaction from the Solace JCA Resource Adapter; hence, the use of Local Transaction support provided by the message broker.

The example EJB deployment descriptor file (ibm-ejb-jar-ext.xml) that configures the Session Bean ‘ProducerSB’ to use Local Transactions instead of XA Transactions.  Refer to [WAS-REF]({{ site.links-was-reference }}){:target="_top"} for further details on WebSphere specific EJB deployment descriptor files.

```xml
<ejb-jar-ext xmlns="http://websphere.ibm.com/xml/ns/javaee"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://websphere.ibm.com/xml/ns/javaee 
    http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-ext_1_0.xsd"
    version="1.0">
    <session name="ProducerSB">
        <local-transaction boundary="BEAN_METHOD"        
                   resolver="CONTAINER_AT_BOUNDARY"/>
    </session>
</ejb-jar-ext>
```


### Working with Solace Disaster Recovery

This section describes configuration required to have a WebSphere Application Server successfully connect to a backup data center using Solace's [Data Center Replication]({{ site.links-docs-replication }}){:target="_top"} feature. 

#### Configuring a Host List within the WebSphere Application Server

As described in [Solace Feature Guide client configuration]({{ site.links-docs-replication-client-config }}){:target="_top"}, the host list provides the address of the backup data center. This is configured within the WebSphere application server through the `ConnectionURL` custom property value (of a respective J2C entity) as follows:

```
tcp://__IP_active_site:PORT__,tcp://__IP_standby_site:PORT__
```

The active site and standby site addresses are provided as a comma-separated list of `Connection URIs`.  When connecting, the Solace JMS connection will first try the active site, and if it's unable to successfully connect to the active site, it will try the standby site. This is discussed in much more detail in the referenced Solace documentation

#### Configuring reasonable JMS Reconnection Properties within Solace JNDI

In order to enable applications to successfully reconnect to the standby site in the event of a data center failure, it is required that the Solace JMS connection be configured to attempt connection reconnection for a sufficiently long time to enable the manual switch-over to occur. The length of time is application specific depending on individual disaster recovery procedures, and can range from minutes to hours depending on the application. In general it's best to tune the reconnection by changing the "reconnect retries" parameter within the Solace JNDI to a value large enough to cover the maximum time to detect and execute a disaster recovery switch over. If this time is unknown, it's also possible to use a value of "-1" to force the Solace JMS API to reconnect indefinitely.

The reconnect retries is tuned for the respective JNDI connection factory on the message broker using the [Solace PubSub+ Manager]({{ site.links-docs-webadmin }}){:target="_top"} admin console or the CLI as follows:

```
(config)# jndi message-vpn solace_VPN
(config-jndi)# connection-factory JNDI/Sol/CF
(config-jndi-connection-factory)# property-list transport-properties
(config-jndi-connection-factory-pl)# property "reconnect-retries" "-1"
(config-jndi-connection-factory-pl)# exit
(config-jndi-connection-factory)# exit
(config-jndi)# exit
(config)#
```

#### Configuring Message Driven Bean Reactivation in the Event of Activation Failures

If a message driven bean is de-activated during a replication failover, the bean may be successfully re-activated to the replication site if the reconnection properties of the bean’s Activation Specification are properly configured.  The default reconnection properties of the J2C activation specification are configured to not re-activate the bean upon de-activation. 

To enable WebSphere to attempt to re-activate the de-activated MDB, configure the reconnection custom properties of the J2C activation specification:

| **Custom Property** | **Default Value** | **Description** |
| reconnectAttempts | 0 | The number of times to attempt to re-activate an MDB after the MDB has failed to activate. |
| reconnectInterval | 10 | The time interval in seconds to wait between attempts to re-activate the MDB. |

##### Receiving Messages in a Message Driven Bean

There is no special processing required during a disaster recovery switch-over specifically for applications receiving messages. After successfully reconnecting to the standby site, it's possible that the application will receive some duplicate messages. The application should apply normal duplicate detection handling for these messages.

##### Sending Messages from a Session Bean

For WebSphere applications that are sending messages, there is nothing specifically required to reconnect the Solace JMS connection. However, any messages that were in the process of being sent will receive an error from the Solace Resource Adapter.  These messages must be retransmitted as possibly duplicated. The application should catch and handle any of the following exceptions:

* javax.resource.spi.SecurityException
* javax.resource.ResourceException or one of its subclasses
* javax.jms.JMSException

### Using an external JNDI store for Solace JNDI lookups

By default the Solace JMS Resource Adapter looks up JNDI objects, which are the Connection Factory and Destination Objects, from the JNDI store on the message broker.

It's possible to use an external JNDI provider such as an external LDAP server instead, and provision the Solace JNDI objects there.

The following configuration changes are required to use an external JNDI provider:

##### Solace JMS Resource Adapter configuration

Refer to the [Configuring the Solace Resource Adapter properties](#configuring-the-solace-resource-adapter-properties) section to compare to the default setup.

The following table summarizes the values used for the resource adapter’s bean properties if using an external JNDI store:

| **Name** | **Value** | **Description** |
| ConnectionURL | PROVIDER_PROTOCOL://IP:Port | The JNDI provider connection URL (Update the value with the actual protocol, IP and port). Example: `ldap://localhost:10389/o=solacedotcom` |
| messageVPN |  | The associated solace message VPN for Connection Factory or Destination Objectis is expected to be stored in the external JNDI store. |
| UserName | jndi_provider_username | The username credential on the external JNDI store (not on the message broker) |
| Password | jndi_provider_password | The password credential on the external JNDI store (not on the message broker) |
| ExtendedProps | java.naming.factory.initial= PROVIDER_InitialContextFactory_CLASSNAME (ensure there is no space used around the = sign) | Substitute `PROVIDER_InitialContextFactory_CLASSNAME` implementing the 3rd party provider InitialContextFactory class with your provider's class name. Example: `com.sun.jndi.ldap.LdapCtxFactory`. Additional Extended Properties Supported Values may be configured as described in the [Solace Resource Adapter properties section](#configuring-the-solace-resource-adapter-properties).

**Important note**: the jar library with the 3rd party provider's implementation of the  "javax.naming.spi.InitialContextFactory" class must be placed in the application server's class path.

<br/>

##### Connection Factories, Activation Specifications and Administered Objects configuration

Refer to the relevant sections to compare to the default setup.

The following table summarizes the values used for custom properties if using an external JNDI store:

| **Name** | **Value** | **Description** |
| connectionFactoryJndiName | CONFIGURED_CF_JNDI_NAME | The JNDI name of the JMS connection factory as configured on the external JNDI store. |
| destination | CONFIGURED_DESTINATION_JNDI_NAME | The JNDI name of the JMS destination as configured on the external JNDI store. |
