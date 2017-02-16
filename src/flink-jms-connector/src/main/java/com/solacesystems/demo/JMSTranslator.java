package com.solacesystems.demo;

import javax.jms.JMSException;
import java.io.Serializable;

/**
 * Minimal message translation interface. Chose abstract class to make all implementations
 * implement Serializable
 *
 * @param <OUT> the target output type for this translator
 */
public abstract class JMSTranslator<OUT> implements Serializable {

    private static final long serialVersionUID = -6297348054233642686L;

    /**
     * Translates the contents of a JMS message into the target output type.
     *
     * @param msg the input JMS message to translate
     * @return translated instance of the target output type
     * @throws JMSException
     */
    public abstract OUT translate(javax.jms.Message msg) throws JMSException;

    /**
     * Used by the SourceFunctions to provide a Type Hint to Flink internals.
     * @return
     */
    public abstract Class<OUT> outputType();
}
