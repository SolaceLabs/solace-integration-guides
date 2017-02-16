package com.solacesystems.demo;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;

@SuppressWarnings("serial")
public class BasicTopicStreamingSample {

    public static void main(String[] args) throws Exception {

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(4);

        final Hashtable<String, String> jmsEnv = new Hashtable<>();
        jmsEnv.put(InitialContext.INITIAL_CONTEXT_FACTORY, "com.solacesystems.jndi.SolJNDIInitialContextFactory");
        jmsEnv.put(InitialContext.PROVIDER_URL, "smf://192.168.56.101");
        jmsEnv.put(Context.SECURITY_PRINCIPAL, "test@poc_vpn");
        jmsEnv.put(Context.SECURITY_CREDENTIALS, "password");

        env.addSource(new JMSTopicSource<String>(jmsEnv,
                "flink_cf",
                "flink/topic",
                new JMSTextTranslator()))
                .print();

        env.execute();
    }

}
