package ru.rudikov.async_adapter_demo;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ru.rudikov.async_adapter_demo.adapter.primary.kafka.ExampleProducer;
import ru.rudikov.async_adapter_demo.application.port.secondary.ScoreDetailsPort;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@Testcontainers
public class ServiceTestCountDownLatch {

    @Autowired
    private ScoreDetailsPort scoreDetailsPort;

    @SpyBean
    private ExampleProducer exampleProducer;

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
        CountDownLatch countDownLatch = new CountDownLatch(1);

        // как только вызывается response producer, вызываем countDown(), чтобы пустить тестовый поток к блоку проверок
        // тем самым гарантируем, что проверка начнется после того, как отправлен ответ клиенту
        doAnswer(
                invocation -> {
                    countDownLatch.countDown();
                    return invocation.callRealMethod();
                }
        ).when(exampleProducer).sendMessage(anyDouble());

        clientProducer.send("request-topic", "key", "123");

        // блочим тестовый поток до тех пор, пока не вызван countDown()
        countDownLatch.await(15, TimeUnit.SECONDS);
        var details = scoreDetailsPort.findAll();

        assertEquals(2, details.size());
    }
}
