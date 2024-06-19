package ru.rudikov.async_adapter_demo;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ru.rudikov.async_adapter_demo.application.port.secondary.ScoreDetailsPort;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
public class ServiceTestThreadSleep {

    @Autowired
    private ScoreDetailsPort scoreDetailsPort;

    @Container
    @ServiceConnection
    private static PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>("postgres:15.2")
                    .withDatabaseName("async_adapter")
                    .withUsername("app")
                    .withPassword("pass");

    @Container
    @ServiceConnection
    private final static KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.3.3")
    );

    // имитация клиентского продюсера
    private static KafkaTemplate<String, String> clientProducer;

    @BeforeAll
    static void setUp() {
        // создаем имитацию клиентского продюсера
        var clientProducerProps = KafkaTestUtils.producerProps(kafkaContainer.getBootstrapServers());
        clientProducerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        clientProducerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        clientProducer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(clientProducerProps));
    }

    @Test
    void example() throws InterruptedException {
        clientProducer.send("request-topic", "key", "123");

        Thread.sleep(15_000);

        var details = scoreDetailsPort.findAll();

        assertEquals(2, details.size());
    }
}