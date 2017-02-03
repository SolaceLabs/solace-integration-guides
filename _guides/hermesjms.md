---
layout: guides
title: HermesJMS
summary: HermesJMS is an extensible console that helps you interact with JMS providers making it simple to publish and edit messages, browse or search queues and topics, copy messages around and delete them.
icon: hermesjms.png
---

## Prerequisites

This guide assumes that:

-	You have successfully installed HermesJMS on your machine (do not use WebStart HermesJMS)
-	You have access to Solace JMS libraries (version 6.2 or above)
-	You have access to a Solace Messaging Router
-	The necessary configuration on the Solace Message Router is done. Configuration includes the creation of elements such as the message-VPN and the JMS Connection Factory.
-	If SSL connectivity is desired, you will need to ensure that the Solace appliance is correctly configured with SSL certificate(s), and that you have obtained a copy of the trust store, and keystore (if necessary) from your administrator.

## Setup Steps

### Modify HermesJMS startup 

#### Windows or Linux

* In the HermesJMS distribution you will find a bin directory with the startup files, hermes.bat and hermes.sh. Edit the file you use to start HermesJMS and add the system property definition `-DSolace_JMS_Browser_Timeout_In_MS=1000` to the Java command. 

#### OS X
1. Open the Applications directory in Finder. Right-click on the HermesJMS icon and select “Show Package Contents”. 
  ![]({{ site.baseurl }}/images/hermesjms/Picture1.png)
1. Go into the Contents subdirectory.
  ![]({{ site.baseurl }}/images/hermesjms/Picture2.png)
1. Double-click to edit the Info.plist file with the (optionally installed) Apple Xcode editor. Add the string `-DSolace_JMS_Browser_Timeout_In_MS=1000` to the Java->VMOptions with a space between the existing options and the new String. Save the change. 
  ![]({{ site.baseurl }}/images/hermesjms/Picture3.png)


### Start HermesJMS and create a new Hermes JMS session
1. Right-click on jms/sessions tree node in the “Sessions” area and select New…/New session… Call it “SolaceSession”. Make sure to uncheck Use Consumer and Transacted.
  ![]({{ site.baseurl }}/images/hermesjms/create-session-1.png)
1. You will be presented with a Preferences dialog which contains 4 tabs. Select the Providers tab.
  ![]({{ site.baseurl }}/images/hermesjms/create-session-2.png)
1. Add a new Classpath group, by right clicking on “Classpath Groups” and selecting Add Group from the popup menu. Type “SolaceJMS” in the Classpath group name text field and hit OK. 
  ![]({{ site.baseurl }}/images/hermesjms/create-session-3.png)
1. Now, add all of the jars in the lib directory of the Solace JMS distribution package, and hit “OK”. A confirmation dialog will be presented (see below). Hit “Don’t scan” to proceed.
  ![]({{ site.baseurl }}/images/hermesjms/create-session-4.png)
1. Hit “Apply” to save the configuration.
  ![]({{ site.baseurl }}/images/hermesjms/create-session-5.png)
1. Switch to the Sessions tab to start configuring the ConnectionFactory.
  ![]({{ site.baseurl }}/images/hermesjms/create-session-6.png)
1. Select the following settings in the drop-down menu for the Connection Factory.
  * For Class,  select hermes.JNDIConnectionFactory
  * For Loader, select SolaceJMS which was previously configured. Sometimes the drop-down menu has not been updated with the SolaceJMS option. If this happens, close the dialog with “OK” and reopen by right clicking the “SolaceJMS” and selecting “edit”.
  ![]({{ site.baseurl }}/images/hermesjms/create-session-7.png)
1. Add the following properties for the Connection Factory:
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
 
1. Hit “OK” to finish creating the new Hermes JMS session.
   
### Create a new Hermes Queue

TODO... Continue from here.