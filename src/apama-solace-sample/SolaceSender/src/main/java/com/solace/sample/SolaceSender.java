package com.solace.sample;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */



import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.XMLMessageProducer;

public class SolaceSender {

	public static void main(String... args) {
		JCSMPSession session = null;
		XMLMessageConsumer cons = null;
		Topic topic = null;
		try {
			// Check command line arguments
			if (args.length < 1) {
				System.out.println("Usage: SolaceSender <msg_backbone_ip:port>");
				System.exit(-1);
			}
			System.out.println("SolaceSender initializing...");

			// Create a JCSMP Session
			final JCSMPProperties properties = new JCSMPProperties();
			properties.setProperty(JCSMPProperties.HOST, args[0]); // msg-backbone
																	// ip:port
			properties.setProperty(JCSMPProperties.VPN_NAME, "apama"); // message-vpn
			properties.setProperty(JCSMPProperties.USERNAME, "apama_user"); // client-username
																			// (assumes
																			// no
																			// password)
			session = JCSMPFactory.onlyInstance().createSession(properties);
			cons = session.getMessageConsumer(new XMLMessageListener() {

				@Override
				public void onException(JCSMPException exception) {
					System.err.println("Error occurred, printout follows.");
					exception.printStackTrace();
				}

				@Override
				public void onReceive(BytesXMLMessage msg) {

					System.out.println("Received message: ");
					System.out.println(msg.dump());
					System.out.println(((TextMessage) msg).getText());
					SDTMap map=msg.getProperties();
					try {
						long ct = map.getLong("MESSAGE_CREATED");
//						System.out.println("Receiving latency : " + (System.currentTimeMillis()-ct));
					} catch (SDTException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			});

			topic = JCSMPFactory.onlyInstance().createTopic("apamaTopic");
			System.out.printf("Setting topic subscription '%s'...\n", topic.getName());
			session.addSubscription(topic);
			// Receive messages.
			cons.start();

			/** Anonymous inner-class for handling publishing events */
			XMLMessageProducer prod = session.getMessageProducer(new JCSMPStreamingPublishEventHandler() {
				public void responseReceived(String messageID) {
					System.out.println("Producer received response for msg: " + messageID);
				}

				public void handleError(String messageID, JCSMPException e, long timestamp) {
					System.out.printf("Producer received error for msg: %s@%s - %s%n", messageID, timestamp, e);
				}
			});
			// Publish-only session is now hooked up and running!
			SDTMap map = prod.createMap();
			map.putString("MESSAGE_TYPE", "com.solace.sample.SampleTextMessage");
			TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
			for (int msgsSent = 0; msgsSent < 1000000; ++msgsSent) {
				//release all memory associated with a message buffer so that the 
				//message buffer is reset to its original state 
				//(that is, as if it has just been allocated). 
				//All fields are reset to their default values
				msg.reset();
				
				msg.setText("msg count is " + String.format("%05d", msgsSent));
				msg.setDeliveryMode(DeliveryMode.DIRECT);
				msg.setApplicationMessageId("appID-" + msgsSent);
				msg.setElidingEligible(true);
				map.putLong("MESSAGE_CREATED", System.currentTimeMillis());

				msg.setProperties(map);
				prod.send(msg, topic);
				Thread.sleep(500);
			}
		} catch (Exception e) {
		} finally {
			if (cons != null)
				cons.stop();
			if (session != null) {
				if (topic != null)
					try {
						session.removeSubscription(topic);
					} catch (JCSMPException e) {
					}
				session.closeSession();
			}

		}

	}
}
