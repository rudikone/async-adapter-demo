package ru.rudikov.async_adapter_demo.adapter.primary.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.rudikov.async_adapter_demo.adapter.primary.kafka.model.ScoreResult;

@Service
public class ExampleProducer {

    Logger logger = LoggerFactory.getLogger(ExampleProducer.class);

    private final KafkaTemplate<String, ScoreResult> kafkaTemplate;

    public ExampleProducer(KafkaTemplate<String, ScoreResult> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String studentId, Double score) {
        final var message = new ScoreResult(studentId, score);
        final var future = kafkaTemplate.send("response-topic", message);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Sent message=[{}]", message);
            } else {
                logger.error("Unable to send message=[{}] due to: {}", message, ex.getMessage());
            }
        });
    }
}
