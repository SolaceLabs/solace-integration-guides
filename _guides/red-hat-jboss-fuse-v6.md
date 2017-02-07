---
layout: guides
title: Red Hat JBoss Fuse v6
summary: Red Hat JBoss Fuse product is an open source Enterprise Service Bus (ESB) that delivers a robust, cost-effective, and open integration platform that lets enterprises easily connect their disparate applications, services, or devices in real time.
icon: red-hat-jboss-fuse.png
---

## Overview

This document demonstrates how to integrate Solace Java Message Service (JMS) with Red Hat JBoss Fuse v6.0 and v6.1 for production and consumption of JMS messages. Red Hat JBoss Fuse supports a variety of different deployment models and dependency injection frameworks. This document describes integration using the OSGi bundle deployment model with the Spring and Blueprint dependency injection frameworks. The goal of this document is to outline best practices for this integration to enable efficient use of both JBoss Fuse and Solace JMS. 

The target audience of this document is developers using JBoss Fuse with knowledge of both JBoss Fuse and JMS in general. As such this document focuses on the technical steps required to achieve the integration. For detailed background on either Solace JMS or JBoss Fuse refer to the referenced documents below.
This document is divided into the following sections to cover the Solace JMS integration with JBoss Fuse:

* Integrating with JBoss Fuse
* Performance Considerations
* Working with Solace High Availability

### Related Documentation

These links contain information related to this guide:

* [Solace Developer Portal]({{ site.links-dev-portal }}){:target="_top"}
* [Solace Messaging API for JMS]({{ site.links-docs-jms }}){:target="_top"}
* [Solace JMS API Online Reference Documentation]({{ site.links-docs-jms-api }}){:target="_top"}
* [Solace Feature Guide]({{ site.links-docs-features }}){:target="_top"}
* [Solace Message Router Configuration]({{ site.links-docs-router-config }}){:target="_top"}
* [Solace Command Line Interface Reference]({{ site.links-docs-cli }}){:target="_top"}
* [RedHat JBoss_Fuse Online reference documentation](https://access.redhat.com/documentation/en-US/Red_Hat_JBoss_Fuse/){:target="_blank"}
* [The Red Hat JBoss Fuse container](https://access.redhat.com/documentation/en-US/Red_Hat_JBoss_Fuse/6.0/html/Deploying_into_the_Container){:target="_blank"}
* [Using the JMS Binding Component](https://access.redhat.com/documentation/en-US/Red_Hat_JBoss_Fuse/6.0/html/Using_the_JMS_Binding_Component){:target="_blank"}

## Integrating with JBoss Fuse

This integration guide demonstrates how to configure JBoss Fuse to send and receive JMS messages using a shared JMS connection. Accomplishing this requires completion of the following steps:

* Step 1 – Configuration of the Solace Appliance.
* Step 2 – Configuring JBoss Fuse to connect to the Solace appliance.
* Step 3 – Configuring JBoss Fuse to send messages using Solace JMS.
* Step 4 – Configuring JBoss Fuse to receive messages using Solace JMS.

### Description of Resources Required

This integration guide will demonstrate creation of Solace resources and configuration of JBoss Fuse managed resources. This section outlines the resources that are created and used in the subsequent sections.

#### Solace Resources

The following Solace appliance resources are required.

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
    <td>Solace_Fuse_VPN</td>
    <td>A Message VPN, or virtual message broker, to scope the integration on the Solace message router.</td>
    </tr>
    <tr>
    <td>Client Username</td>
    <td>fuse_user</td>
    <td>The client username.</td>
    </tr>
    <tr>
    <td>Client Password</td>
    <td>fuse_password</td>
    <td>Optional client password. </td>
    </tr>
    <tr>
    <td>Solace Queue</td>
    <td>Q/requests</td>
    <td>Solace destination of messages produced and consumed</td>
    </tr>
    <tr>
    <td>JNDI Connection Factory</td>
    <td>JNDI/CF/fuse</td>
    <td>The JNDI Connection factory for controlling Solace JMS connection properties</td>
    </tr>
    <tr>
    <td>JNDI Queue Name</td>
    <td>JNDI/Q/requests</td>
    <td>The JNDI name of the queue used in the samples</td>
    </tr>
</table>

#### JBoss Fuse Configuration Resources

<table>
    <tr>
    <th>Resource</th>
    <th>Value</th>
    </tr>
    <tr>
    <td>JndiTemplate</td>
    <td>Solace.JndiTemplate</td>
    </tr>
    <tr>
    <td>JndiObjectFactoryBean</td>
    <td>Solace.JndiObjectFactoryBean</td>
    </tr>
    <tr>
    <td>JmsComponent</td>
    <td>Solace.JmsComponent</td>
    </tr>
</table>

### Step 1 – Configuring the Solace Appliance

The Solace appliance needs to be configured with the following configuration objects at a minimum to enable JMS to send and receive messages within JBoss Fuse:

* A Message VPN, or virtual message broker, to scope the integration on the Solace appliance.
* Client connectivity configurations like usernames and profiles
* Guaranteed messaging endpoints for receiving messages.
* Appropriate JNDI mappings enabling JMS clients to connect to the Solace appliance configuration.

For reference, the CLI commands in the following sections are from SolOS version 6.2 but will generally be forward compatible. For more details related to Solace appliance CLI see [Solace-CLI]. Wherever possible, default values will be used to minimize the required configuration. The CLI commands listed also assume that the CLI user has a Global Access Level set to Admin. For details on CLI access levels please see [Solace-FG] section “User Authentication and Authorization”.

Also note that this configuration can also be easily performed using SolAdmin, Solace’s GUI management tool. This is in fact the recommended approach for configuring a Solace appliance. This document uses CLI as the reference to remain concise.

#### Creating a Message VPN
This section outlines how to create a message-VPN called “Solace_Fuse_VPN” on the Solace appliance with authentication disabled and 2GB of message spool quota for Guaranteed Messaging. This message-VPN name is required in JBoss Fuse configuration when connecting to the Solace messaging appliance. In practice appropriate values for authentication, message spool and other message-VPN properties should be chosen depending on the end application’s use case. 

```
(config)# create message-vpn Solace_Fuse_VPN
(config-msg-vpn)# authentication
(config-msg-vpn-auth)# user-class client
(config-msg-vpn-auth-user-class)# basic auth-type none
(config-msg-vpn-auth-user-class)# exit
(config-msg-vpn-auth)# exit
(config-msg-vpn)# no shutdown
(config-msg-vpn)# exit
(config)#
(config)# message-spool message-vpn Solace_Fuse_VPN
(config-message-spool)# max-spool-usage 2000
(config-message-spool)# exit
(config)#
```

#### Configuring Client Usernames & Profiles

This section outlines how to update the default client-profile and how to create a client username for connecting to the Solace appliance. For the client-profile, it is important to enable guaranteed messaging for JMS messaging and transacted sessions if using transactions. 

The chosen client username of “fuse_user” will be required by JBoss Fuse when connecting to the Solace appliance.

```
(config)# client-profile default message-vpn Solace_Fuse_VPN
(config-client-profile)# message-spool allow-guaranteed-message-receive
(config-client-profile)# message-spool allow-guaranteed-message-send
(config-client-profile)# message-spool allow-transacted-sessions
(config-client-profile)# exit
(config)#
(config)# create client-username fuse_user message-vpn Solace_Fuse_VPN
(config-client-username)# acl-profile default    
(config-client-username)# client-profile default
(config-client-username)# no shutdown
(config-client-username)# exit
(config)#
```

#### Setting up Guaranteed Messaging Endpoints

This integration guide shows receiving messages within JBoss Fuse from a single JMS Queue. For illustration purposes, this queue is chosen to be an exclusive queue with a message spool quota of 2GB matching quota associated with the message VPN. The queue name chosen is “Q/requests”.

```
(config)# message-spool message-vpn Solace_Fuse_VPN
(config-message-spool)# create queue Q/requests
(config-message-spool-queue)# access-type exclusive
(config-message-spool-queue)# max-spool-usage 2000
(config-message-spool-queue)# permission all delete
(config-message-spool-queue)# no shutdown
(config-message-spool-queue)# exit
(config-message-spool)# exit
(config)#
```

### Setting up Solace JNDI References

To enable the JMS clients to connect and look up the Queue destination required by JBoss Fuse, there are two JNDI objects required on the Solace appliance:

* A connection factory: JNDI/CF/fuse
* A queue destination: JNDI/Q/requests

They are configured as follows:

```
(config)# jndi message-vpn Solace_Fuse_VPN
(config-jndi)# create connection-factory JNDI/CF/fuse
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
(config-jndi)# create queue JNDI/Q/requests
(config-jndi-queue)# property physical-name Q/requests
(config-jndi-queue)# exit
(config-jndi)# 
(config-jndi)# no shutdown
(config-jndi)# exit
(config)#
```

### Step 2 – JBoss Fuse – Connecting

TODO...



