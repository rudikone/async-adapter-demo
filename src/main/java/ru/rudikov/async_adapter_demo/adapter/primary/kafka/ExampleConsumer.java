package ru.rudikov.async_adapter_demo.adapter.primary.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.rudikov.async_adapter_demo.application.port.primary.ScoringPort;


@Service
public class ExampleConsumer {

    Logger logger = LoggerFactory.getLogger(ExampleConsumer.class);

    private final ScoringPort scoringPort;
    private final ExampleProducer producer;

    public ExampleConsumer(ScoringPort scoringPort, ExampleProducer producer) {
        this.scoringPort = scoringPort;
        this.producer = producer;
    }

    @KafkaListener(topics = "request-topic")
    public void listen(String request) {
        logger.info("Received studentId: {}", request);

        try {
            var score = scoringPort.getScore(request);
            producer.sendMessage(score);
        } catch (Throwable throwable) {
            logger.error("Error processing request with studentId: {}", request, throwable);
        }
    }
}
