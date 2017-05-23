# NiFi JNDI Connection Factory Provider

The NiFi 'nifi-jms-bundle' has 2 built-in processors, 'ConsumeJMS' and 'PublishJMS'. There is one built-in JMS connection factory provider that instantiates zero-arugment connection factory objects. Some connection factories cannot be instantiated with zero-arugments. The alternative is to use the standard JMS JNDI connection factory. Most of JMS brokers do support JNDI connection factory.

The project is based on 'nifi-jms-bundle' from NiFi tag 'rel/1.1.2'. 'nifi-jms-bundle' has 2 sub projects, 'nifi-jms-cf-service' and 'nifi-jms-processors'. 'nifi-jms-cf-service' is referenced by 'nifi-jms-processors'.

## Modifications and Changes
Changes are made as following:

1. New code
  
    * nifi-jms-cf-service\src\main\java\org\apache\nifi\jms\cf\JNDIConnectionFactoryProvider.java
    * nifi-jms-cf-service\src\test\java\org\apache\nifi\jms\cf\JNDIConnectionFactoryProviderTest.java

2. Modified code

    * nifi-jms-cf-service\src\main\java\org\apache\nifi\jms\cf\JMSConnectionFactoryProviderDefinition.java
    * nifi-jms-cf-service\src\main\resources\META-INF\services\org.apache.nifi.controller.ControllerService
    * nifi-jms-processors\src\test\java\org\apache\nifi\jms\processors\CommonTest.java
    * nifi-jms-processors\src\test\java\org\apache\nifi\jms\processors\ConsumeJMSTest.java
    * nifi-jms-processors\src\test\java\org\apache\nifi\jms\processors\JMSPublisherConsumerTest.java
    * nifi-jms-processors\src\test\java\org\apache\nifi\jms\processors\PublishJMSTest.java
  
## Build procedures
The maven build script is executed from 'nifi-jms-bundle' directory as following:
```
 $ mvn clean test nifi-nar:nar
[INFO] Scanning for projects...
[INFO] Inspecting build with total of 5 modules...
[INFO] Installing Nexus Staging features:
[INFO]   ... total of 5 executions of maven-deploy-plugin replaced with nexus-staging-maven-plugin
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Build Order:
[INFO]
[INFO] nifi-jms-bundle
[INFO] nifi-jms-cf-service
[INFO] nifi-jms-cf-service-nar
[INFO] nifi-jms-processors
[INFO] nifi-jms-processors-nar
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building nifi-jms-bundle 1.1.2
[INFO] ------------------------------------------------------------------------

...

[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary:
[INFO]
[INFO] nifi-jms-bundle .................................... SUCCESS [  0.981 s]
[INFO] nifi-jms-cf-service ................................ SUCCESS [  6.345 s]
[INFO] nifi-jms-cf-service-nar ............................ SUCCESS [  0.705 s]
[INFO] nifi-jms-processors ................................ SUCCESS [  7.412 s]
[INFO] nifi-jms-processors-nar ............................ SUCCESS [  1.391 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 18.709 s
[INFO] Finished at: 2017-05-13T17:22:28-04:00
[INFO] Final Memory: 27M/1064M
[INFO] ------------------------------------------------------------------------
Java HotSpot(TM) 64-Bit Server VM warning: ignoring option MaxPermSize=256m; support was removed in 8.0
```
## Code Deployment
After build script is finished, copy 2 new NAR files to ~/nifi-1.1.2/lib
  * nifi-jms-bundle/nifi-jms-cf-service-nar/target/nifi-jms-cf-service-nar-1.1.2.nar
  * nifi-jms-bundle/nifi-jms-processors-nar/target/nifi-jms-processors-nar-1.1.2.nar

## NiFi Restart
Upon restart, NiFi shall detect 2 new NARs and reload them.
```
2017-05-12 22:00:20,282 INFO [main] org.apache.nifi.NiFi Loaded 121 properties
2017-05-12 22:00:20,488 INFO [main] org.apache.nifi.BootstrapListener Started Bootstrap Listener, Listening for incoming requests on port 57824
2017-05-12 22:00:20,503 INFO [main] org.apache.nifi.BootstrapListener Successfully initiated communication with Bootstrap
2017-05-12 22:00:22,664 INFO [main] org.apache.nifi.nar.NarUnpacker Contents of nar c:\proj\NIFI-1~1.2\.\lib\nifi-jms-cf-service-nar-1.1.2.nar have changed. Reloading.
2017-05-12 22:00:22,870 INFO [main] org.apache.nifi.nar.NarUnpacker Contents of nar c:\proj\NIFI-1~1.2\.\lib\nifi-jms-processors-nar-1.1.2.nar have changed. Reloading.
```
## NiFi Config
Up to this point, the latest JNDI connection provider is available in NiFi. By setting up JMS broker specific properties, the JNDI connection factory provider can work with broader set of JMS brokers, i.e. Solace, ActiveMQ and etc.
