package ru.rudikov.async_adapter_demo.adapter.primary.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ExampleProducer {

    Logger logger = LoggerFactory.getLogger(ExampleProducer.class);

    private final KafkaTemplate<String, Double> kafkaTemplate;

    public ExampleProducer(KafkaTemplate<String, Double> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(Double message) {
        CompletableFuture<SendResult<String, Double>> future = kafkaTemplate.send("response-topic", message);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Sent message=[{}]", message);
            } else {
                logger.error("Unable to send message=[{}] due to: {}", message, ex.getMessage());
            }
        });
    }
}
