@TransactionManagement(value = TransactionManagementType.CONTAINER)
@TransactionAttribute(value = TransactionAttributeType.REQUIRED)

public class XAConsumerMDB implements MessageListener {

    @EJB(beanName = "XAProducerSB", beanInterface=Producer.class)
    Producer sb;

    public XAConsumerMDB() { }

    public void onMessage(Message message) {
    	 String msg = message.toString();

        System.out.println(Thread.currentThread().getName() + " - XAConsumerMDB: received message: " + msg);

        try {
        	// Send reply message
        	sb.sendMessage();

        } catch (JMSException e) {
        		throw new EJBException("Error while sending reply message", e);
        }  
     }  
}
