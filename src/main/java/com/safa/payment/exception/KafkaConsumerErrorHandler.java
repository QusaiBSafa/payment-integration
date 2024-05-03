
package com.safa.payment.exception;


import org.apache.kafka.clients.consumer.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.ConsumerAwareListenerErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component(value = "KafkaConsumerErrorHandler")
public class KafkaConsumerErrorHandler implements ConsumerAwareListenerErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerErrorHandler.class);

    @Override
    public Object handleError(Message<?> message, ListenerExecutionFailedException exception, Consumer<?, ?> consumer) {
        logger.error("Error while consuming events  : {}, because : {}", message.getPayload(),
                exception.getMessage());
        if (exception.getCause() instanceof RuntimeException) {
            throw exception;
        }
        return null;
    }

}
