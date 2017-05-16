@TransactionManagement(value = TransactionManagementType.BEAN)
@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)

public class ConsumerMDB implements MessageListener {

    @EJB(beanName = "ProducerSB", beanInterface=Producer.class)
    Producer sb;

    public ConsumerMDB() { }

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
