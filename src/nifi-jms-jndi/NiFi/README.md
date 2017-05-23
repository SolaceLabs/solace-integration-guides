# NiFi JMS Processor Configuration

NiFi keeps all the GUI widget configurations in a file '~/nifi-1.1.2/conf/flow.xml.gz/flow.xml'. NiFi's 2 built-in JMS processors are bundled together and can share the same JMS connection factory provider instance.

## Solace Provider Sharing View
![Provider Sharing](/resources/providerSetting.png)

## The Solace JNDI connection factory properties is configured as:

![Provider properties](/resources/providerProp.png)

## The Solace provider properties in the flow file are as below:
```xml
    <controllerService>
      <id>fa5ff488-015b-1000-5674-9ddc019790ae</id>
      <name>JNDIConnectionFactoryProvider</name>
      <comment/>
      <class>org.apache.nifi.jms.cf.JNDIConnectionFactoryProvider</class>
      <enabled>true</enabled>
      <property>
        <name>cf</name>
        <value>com.solacesystems.jndi.SolJNDIInitialContextFactory</value>
      </property>
      <property>
        <name>cflib</name>
        <value>c:\proj\soljar</value>
      </property>
      <property>
        <name>broker</name>
        <value>smf://nifi@192.168.56.101:55555</value>
      </property>
      <property>
        <name>cfname</name>
        <value>/jms/cf/default</value>
      </property>
      <property>
        <name>SSL Context Service</name>
      </property>
    </controllerService>
 ```
    
