@TransactionManagement(value = TransactionManagementType.CONTAINER)
@TransactionAttribute(value = TransactionAttributeType.REQUIRED)

// Perform JNDI lookups on the Weblogic's local JNDI store
@MessageDriven(
		activationConfig = { 
				@ActivationConfigProperty(
						propertyName="connectionFactoryJndiName", 
						propertyValue="JNDI/J2C/CF"),
				@ActivationConfigProperty(
						propertyName="destinationType", 
						propertyValue="javax.jms.Queue"),
				@ActivationConfigProperty(
						propertyName="destinationJndiName", 
						propertyValue="JNDI/J2C/Q/requests")
		})

//// Alternative activation configuration to perform JNDI lookups 
//// on the Solace router by specifying the resource adapter's JNDI name
//@MessageDriven(
//		activationConfig = { 
//				@ActivationConfigProperty(
//						propertyName="connectionFactoryJndiName", 
//						propertyValue="JNDI/Sol/CF"),
//				@ActivationConfigProperty(
//						propertyName="destinationType", 
//						propertyValue="javax.jms.Queue"),
//				@ActivationConfigProperty(
//						propertyName="destination", 
//						propertyValue="JNDI/Sol/Q/requests"), 
//				@ActivationConfigProperty(
//						propertyName="resourceAdapterJndiName", 
//						propertyValue="JNDI/J2C/RA/sol-jms-ra")
//		})

public class XAConsumerMDB implements MessageListener {

	@EJB(beanName = "XAProducerSB", beanInterface=XAProducerSBLocal.class)
	XAProducerSBLocal sb;

	public XAConsumerMDB() { }

	public void onMessage(Message message) {
		String msg = message.toString();

		System.out.println(Thread.currentThread().getName() + 
				" - XAConsumerMDB: received message: " + msg);

		try {
			// Send reply message
			sb.sendMessage();

		} catch (JMSException e) {
			throw new EJBException("Error while sending reply message", e);
		}  
	}  
}
