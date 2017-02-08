---
layout: guides
title: HermesJMS
summary: HermesJMS is an extensible console that helps you interact with JMS providers making it simple to publish and edit messages, browse or search queues and topics, copy messages around and delete them.
icon: hermesjms.png
links:
    - label: Blog Post - Browsing Messages on Solace using HermesJMS
      link: https://solace.com/blog/devops/browsing-messages-solace-using-hermesjms
---

## Prerequisites

This guide assumes that:

-	You have successfully installed HermesJMS on your machine (do not use [WebStart HermesJMS](http://www.hermesjms.com/webstart/hermes/bin/hermes.jnlp){:target="_blank"})
-	You have access to Solace JMS libraries (version 6.2 or above)
-	You have access to a Solace Messaging Router
-	The necessary configuration on the Solace Message Router is done. Configuration includes the creation of elements such as the message-VPN and the JMS Connection Factory.
-	If SSL connectivity is desired, you will need to ensure that the Solace appliance is correctly configured with SSL certificate(s), and that you have obtained a copy of the trust store, and keystore (if necessary) from your administrator.

## Setup Steps

### Modify HermesJMS startup 

#### Windows or Linux

* In the HermesJMS distribution you will find a bin directory with the startup files, hermes.bat and hermes.sh. Edit the file you use to start HermesJMS and add the system property definition `-DSolace_JMS_Browser_Timeout_In_MS=1000` to the Java command. 

#### OS X
Step 1. Open the Applications directory in Finder. Right-click on the HermesJMS icon and select “Show Package Contents”. 
  
![]({{ site.baseurl }}/images/hermesjms/Picture1.png)

Step 2. Go into the Contents subdirectory.

![]({{ site.baseurl }}/images/hermesjms/Picture2.png)

Step 3. Double-click to edit the Info.plist file with the (optionally installed) Apple Xcode editor. Add the string `-DSolace_JMS_Browser_Timeout_In_MS=1000` to the Java->VMOptions with a space between the existing options and the new String. Save the change. 
  
![]({{ site.baseurl }}/images/hermesjms/Picture3.png)


### Start HermesJMS and create a new Hermes JMS session
Step 1. Right-click on jms/sessions tree node in the “Sessions” area and select New…/New session… Call it “SolaceSession”. Make sure to uncheck `Use Consumer and Transacted.`

![]({{ site.baseurl }}/images/hermesjms/create-session-1.png)

Step 2. You will be presented with a Preferences dialog which contains 4 tabs. Select the Providers tab.

![]({{ site.baseurl }}/images/hermesjms/create-session-2.png)

Step 3. Add a new Classpath group, by right clicking on “Classpath Groups” and selecting Add Group from the popup menu. Type “SolaceJMS” in the Classpath group name text field and hit OK. 
  
![]({{ site.baseurl }}/images/hermesjms/create-session-3.png)

Step 4. Now, add all of the jars in the lib directory of the Solace JMS distribution package, and hit “OK”. A confirmation dialog will be presented (see below). Hit “Don’t scan” to proceed.
  
![]({{ site.baseurl }}/images/hermesjms/create-session-4.png)

Step 5. Hit “Apply” to save the configuration.

![]({{ site.baseurl }}/images/hermesjms/create-session-5.png)

Step 6. Switch to the Sessions tab to start configuring the ConnectionFactory.

![]({{ site.baseurl }}/images/hermesjms/create-session-6.png)

Step 7. Select the following settings in the drop-down menu for the Connection Factory.
  * For Class,  select hermes.JNDIConnectionFactory
  * For Loader, select SolaceJMS which was previously configured. Sometimes the drop-down menu has not been updated with the SolaceJMS option. If this happens, close the dialog with “OK” and reopen by right clicking the “SolaceJMS” and selecting “edit”.
  
![]({{ site.baseurl }}/images/hermesjms/create-session-7.png)

Step 8. Add the following properties for the Connection Factory:

<table>
    <tr>
    <th>Property</th>
    <th>Value</th>
    </tr>
    <tr>
    <td>binding</td>
    <td>JNDI name of the connection factory you want to use</td>
    </tr>
    <tr>
    <td>initialContextFactory</td>
    <td>com.solacesystems.jndi.SolJNDIInitialContextFactory</td>
    </tr>
    <tr>
    <td>providerURL</td>
    <td>smf://HOST_IP[:PORT]</td>
    </tr>
    <tr>
    <td>securityPrincipal</td>
    <td>CLIENT_USER_NAME@MSG_VPN_NAME</td>
    </tr>
    <tr>
    <td>securityCredentials</td>
    <td>PASSWORD</td>
    </tr>
</table>
  
![]({{ site.baseurl }}/images/hermesjms/create-session-8.png)
 
Step 9. Hit “OK” to finish creating the new Hermes JMS session.
   
### Create a new Hermes Queue

a)	Right-click on jms/sessions/SolaceSession tree node in the “Sessions” area and select New…/Add queue…

![]({{ site.baseurl }}/images/hermesjms/create-queue-1.png)

b)	Input the following properties:

<table>
    <tr>
    <th>Property</th>
    <th>Value</th>
    </tr>
    <tr>
    <td>Name</td>
    <td>JNDI name of the queue that you wish to connect to</td>
    </tr>
    <tr>
    <td>ShortName</td>
    <td>Any desired display name. For simplicity, this example will keep it as the same as the JNDI name.</td>
    </tr>
</table>

![]({{ site.baseurl }}/images/hermesjms/create-queue-2.png)

c)	Hit “OK” to finish creating the new Hermes JMS queue.

###	Test the setup

a)	To browse the message in the queue added in the previous setup, simply double click on it in the tree on the left hand side. If the queue contains messages they will be displayed in a queue tab as shown below

![]({{ site.baseurl }}/images/hermesjms/test-setup-1.png)

###	Configure Hermes JMS session to connect securely over SSL

a)	Using your preferred text editor, create a new file to pass additional user properties to HermesJMS. For this example, the name of the properties file will be “solace.jms.properties”.

b)	Insert the following two lines of text into your file. Note that the text should be modified to point to the location of your trust store, and must also contain the password of your trust store.

```
Solace_JMS_SSL_TrustStore=C:\\JMS\\HermesJMS\\truststore.jks
Solace_JMS_SSL_TrustStorePassword=myTrustStorePassword
```

c)	Edit the Hermes JMS session in [Start HemesJMS](#start-hermesjms-and-create-a-new-hermes-jms-session) to have the session connect securely over SSL. Right-click on jms/sessions/SolaceSession tree node in the “Sessions” area and select Edit…

![]({{ site.baseurl }}/images/hermesjms/connect-ssl-1.png)

d)	Edit the providerURL property to connect to `smfs://<appliance message backbone IP>:[<appliance SSL port>]`.

e)	Add “userPropertiesFile” property to the connection factory. This value of this property must be the full file name of the solace.jms.properties file that was created earlier in step (a).

![]({{ site.baseurl }}/images/hermesjms/connect-ssl-2.png)

###	Configure Hermes JMS session to connect using client certificate authentication

a)	[“Configure Hermes JMS session to connect securely over SSL”](#configure-hermes-jms-session-to-connect-securely-over-ssl) must be done prior to enabling client certificate authentication.

b)	Edit the solace.jms.properties custom user property file to specify additional properties, modifying them to point to your actual files, and passwords as necessary.

```
Solace_JMS_SSL_TrustStore=C:\\JMS\\HermesJMS\\truststore.jks
Solace_JMS_SSL_TrustStorePassword=myTrustStorePassword

Solace_JMS_Authentication_Scheme=AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE
Solace_JMS_SSL_KeyStore=C:\\JMS\\HermesJMS\\keystore.jks
Solace_JMS_SSL_KeyStorePassword=myKeyStorePassword
Solace_JMS_SSL_PrivateKeyAlias=myPrivateKeyAlias
Solace_JMS_SSL_PrivateKeyPassword=myPrivateKeyPassword
```

Here is a brief summary of the properties used, but you should refer to the Solace JMS API guide for full details.

<table>
    <tr>
    <th>Property</th>
    <th>Description</th>
    </tr>
    <tr>
    <td>Solace_JMS_SSL_TrustStore</td>
    <td>The trust store to use. </td>
    </tr>
    <tr>
    <td>Solace_JMS_SSL_TrustStorePassword</td>
    <td>The trust store password for the trust store provided for the SSL Trust Store property.</td>
    </tr>
    <tr>
    <td>Solace_JMS_Authentication_Scheme</td>
    <td>This property specifies the authentication scheme to be used.</td>
    </tr>
    <tr>
    <td>Solace_JMS_SSL_KeyStore</td>
    <td>This property specifies the keystore to use in the URL or path format. The keystore holds the client’s private key and certificate required to authenticate a client during the TLS/SSL handshake.</td>
    </tr>
    <tr>
    <td>Solace_JMS_SSL_KeyStorePassword</td>
    <td>This property specifies keystore password to use. This password allows JMS to verify the integrity of the keystore.</td>
    </tr>
    <tr>
    <td>Solace_JMS_SSL_PrivateKeyAlias</td>
    <td>This property specifies which private key in the keystore to use for authentication. This property is necessary when a keystore with multiple private key entries is used.</td>
    </tr>
    <tr>
    <td>Solace_JMS_SSL_PrivateKeyPassword</td>
    <td>This property specifies the password for the private key in the keystore.</td>
    </tr>
</table>


