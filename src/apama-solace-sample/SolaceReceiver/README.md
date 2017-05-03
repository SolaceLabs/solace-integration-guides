# Solace Message Receiver #

This Apama project is generated in Software AG Apama Community Version 9.12. The purpose of the project is to demonstrator how Apama can receive and map messages from Solace into Apama events. 

## Project highlights: ##

**Source message format:**

1. TextMessage is used to pass strings in JMS body
2. Solace SDTMap is used to store message properties

**Target event definition:** 

1. Payload string field is mapped to JMS body to receive JMS body texts
2. ExtraParam dictionary field is mapped to JMS message property to receive key-value pairs 

**Message to Event mapping key:**
* MESSAGE_TYPE in JMS property must identify the target event name including full package name
 

**Build requirement:**

1. Java 8
2. Software AG Apama Community Version 9.12 or above
3. Gradle version 3 or above

**Build procedure:** 

1. Clone from GIT using CLI or Eclipse. 
2. Run Gradle task 'gradlew copyToLib' to populate Solace library jars from Maven central to 'libs' directory
3. Import the project into Software AG Design Studio, make sure check on 'copy project to workspace'
4. Switch to 'Apama Workbench' perspective
5. Select 'SolaceReceiver' as current project from the dropdown box
6. Click on 'Play' to run the project
7. The correlator shall start successfully as logs are scrolling in Console

**Runtime requirement:** 

1. Solace VMR or appliance
2. A defined JNDI name mapped to a VPN
3. A client profile that allows GM send/receive, endpoint create and transacted sessions


**Log output shows received JMS message:** 

```text
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


