package com.solacesystems.demo;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

public class JMSTextTranslator extends JMSTranslator<String> {
    @Override
    public String translate(Message msg) throws JMSException {
        TextMessage txtmsg = (TextMessage) msg;
        return txtmsg.getText();
    }
    @Override
    public Class<String> outputType() {
        return String.class;
    }
}
