@Stateless(name = "ProducerSB")
@TransactionManagement(value = TransactionManagementType.BEAN)
@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)

public class ProducerSB implements Producer, ProducerLocal
{
	@Resource(name = "myCF")
	ConnectionFactory myCF;
	
	@Resource(name = "myReplyQueue")
	Queue myReplyQueue;

	public ProducerSB() { }
	
	@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)
	@Override
	public void sendMessage() throws JMSException {
		
       		System.out.println("Sending reply message");
		Connection conn = null;
		Session session = null;
		MessageProducer prod = null;
		
		try {
			conn = myCF.createConnection();
			session = conn.createSession(true, Session.AUTO_ACKNOWLEDGE);
			prod = session.createProducer(myReplyQueue);
			
			ObjectMessage msg = session.createObjectMessage();
			msg.setObject("Hello world!");
			prod.send(msg, DeliveryMode.PERSISTENT, 0, 0);
		} 
		finally {
			if (prod != null) prod.close();
			if (session != null) session.close();
			if (conn != null) conn.close();
		}
	}
}
