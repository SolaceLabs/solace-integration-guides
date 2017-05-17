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
package com.solace.sample;

import com.solacesystems.jms.SolJmsUtility;

import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

import com.solacesystems.jms.SupportedProperty;
import java.util.Properties;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

public class QueuePubSubJNDI {

    private static final String ORIGINATION_TIME = "OriginationTime";

    public void run(String... args) throws Exception {
        int count = 10;

        System.out.println("QueuePubSubJNDI initializing...");

        // The client needs to specify all of the following properties:
        Properties env = new Properties();
        env.put(InitialContext.INITIAL_CONTEXT_FACTORY, "com.solacesystems.jndi.SolJNDIInitialContextFactory");
        env.put(InitialContext.PROVIDER_URL, (String) args[0]);
        try {
            int i = 0;
            if (args.length > 1) {
                i = Integer.parseInt(args[1]);
            }
            if (i > 0) {
                count = i;
            }

        } catch (Exception e) {
        }
        env.put(SupportedProperty.SOLACE_JMS_VPN, "default");
        env.put(Context.SECURITY_PRINCIPAL, "nifi");
        env.put(Context.SECURITY_CREDENTIALS, "");

        // InitialContext is used to lookup the JMS administered objects.
        InitialContext initialContext = new InitialContext(env);
        // Lookup ConnectionFactory.
        ConnectionFactory cf = (ConnectionFactory) initialContext.lookup("/jms/cf/default");
        // JMS Connection
        Connection connection = cf.createConnection();

        // Create a non-transacted, Auto Ack session.
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // Lookup Queue.
        Queue qPub = (Queue) initialContext.lookup("/JNDI/Q/toNifi");

        // Lookup Queue.
        Queue qSub = (Queue) initialContext.lookup("/JNDI/Q/fromNifi");
        // From the session, create a consumer for the destination.
        MessageConsumer consumer = session.createConsumer(qSub);
        /**
         * Anonymous inner-class for receiving messages *
         */
        consumer.setMessageListener(new MessageListener() {

            @Override
            public void onMessage(Message message) {
                try {
                    if (message instanceof TextMessage) {
                        System.out.printf("TextMessage received: '%s'%n", ((TextMessage) message).getText());
                    } else {
                        System.out.println("Message received.");
                    }
                    System.out.printf("Message Dump:%n%s%n", SolJmsUtility.dumpMessage(message));

                    long tmStart = message.getLongProperty(ORIGINATION_TIME);
                    System.out.printf("appID = %d, latency = %d ms %n", message.getLongProperty("appID"), (System.currentTimeMillis() - tmStart));

                } catch (JMSException e) {
                    System.out.println("Error processing incoming message.");
                    e.printStackTrace();
                }
            }
        });
        
        // Do not forget to start the JMS Connection.
        connection.start();

        // Output a message on the console.
        System.out.println("Waiting for a message ... (press Ctrl+C) to terminate ");
        // From the session, create a producer for the destination.
        // Use the default delivery mode as set in the connection factory
        MessageProducer producer = session.createProducer(qPub);

        // Create a text message.
        TextMessage message = session.createTextMessage("Hello world Queues!");
        message.setBooleanProperty(SupportedProperty.SOLACE_JMS_PROP_DELIVER_TO_ONE, false);

        System.out.printf("Connected. About to send message '%s' to queue '%s'...%n", message.getText(),
                qPub.toString());

        for (int i = 1; i <= count; i++) {
            long t = System.currentTimeMillis();
            message.setLongProperty("appID", i);
            message.setLongProperty(ORIGINATION_TIME, t);
            producer.send(qPub, message, DeliveryMode.PERSISTENT, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE);
            System.out.printf("Message %d is sent at %d %n", i, t);
            try {
                Thread.sleep(100);
            } catch (Exception ex) {
            }
        }

        System.out.println("Messages sent. Exiting.");
        try {
            Thread.sleep(1000);
        } catch (Exception ex) {
        }
        connection.close();
        initialContext.close();
    }

    public static void main(String... args) throws Exception {

        // Check command line arguments
        if (args.length < 1) {
            System.out.println("Usage: QueuePubSubJNDI <msg_backbone_ip:port> [message count]");
            System.exit(-1);
        }

        QueuePubSubJNDI app = new QueuePubSubJNDI();
        app.run(args);
    }
}
