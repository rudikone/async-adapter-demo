package ru.rudikov.async_adapter_demo;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import ru.rudikov.async_adapter_demo.adapter.primary.kafka.model.ScoreResult;
import ru.rudikov.async_adapter_demo.application.port.secondary.ScoreDetailsPort;

import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL;

public class AwaitilityTest extends BaseServiceTest {

    @Autowired
    private ScoreDetailsPort scoreDetailsPort;

    // имитация клиентского продюсера
    private static KafkaTemplate<String, String> clientProducer;

    // имитация клиентского консюмера
    private static KafkaMessageListenerContainer<String, ScoreResult> clientConsumer;
    // блокирующая очередь с размером 1, чтобы гарантировать последовательную обработку сообщений во всех тестах
    private static final BlockingQueue<ConsumerRecord<String, ScoreResult>> clientConsumerRecords =
            new LinkedBlockingQueue<>(1);

    @BeforeAll
    static void setUp() {
        // создаем имитацию клиентского продюсера
        var clientProducerProps = KafkaTestUtils.producerProps(kafkaContainer.getBootstrapServers());
        clientProducerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        clientProducerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        clientProducer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(clientProducerProps));

        // создаем имитацию клиентского консюмера
        var clientConsumerProps = new HashMap<String, Object>();
        clientConsumerProps.put(BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        clientConsumerProps.put(GROUP_ID_CONFIG, "consumer");
        clientConsumerProps.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        clientConsumerProps.put(ALLOW_AUTO_CREATE_TOPICS_CONFIG, "false");
        clientConsumerProps.put(MAX_POLL_RECORDS_CONFIG, "1");
        clientConsumerProps.put(ENABLE_AUTO_COMMIT_CONFIG, "false");
        clientConsumerProps.put(MAX_POLL_INTERVAL_MS_CONFIG, "15000"); // больше времени ответа сервиса

        var deserializer = new JsonDeserializer<ScoreResult>();
        deserializer.addTrustedPackages("*");
        var clientConsumerFactory = new DefaultKafkaConsumerFactory<>(
                clientConsumerProps,
                new StringDeserializer(),
                deserializer
        );
        var clientConsumerContainerProperties = new ContainerProperties("response-topic");
        clientConsumerContainerProperties.setAckMode(MANUAL);

        clientConsumer = new KafkaMessageListenerContainer<>(clientConsumerFactory, clientConsumerContainerProperties);
        // задаем поведение клиентского консюмера
        clientConsumer.setupMessageListener(
                (AcknowledgingMessageListener<String, ScoreResult>) (data, acknowledgment) -> {
                    try {
                        clientConsumerRecords.put(data);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    requireNonNull(acknowledgment).acknowledge();
                }
        );
        clientConsumer.start();
    }

    @AfterAll
    static void stop() {
        clientConsumer.stop();
    }

    @Test
    void example1() {
        // имитация клиентского запроса
        var studentId = "123";
        clientProducer.send("request-topic", "key", studentId);

        await()
                .pollInterval(Duration.ofSeconds(3))
                .atMost(15, SECONDS)
                .untilAsserted(() -> {
                    var response = Optional.ofNullable(clientConsumerRecords.poll());
                    var details = scoreDetailsPort.findAllByStudentId(studentId);

                    assertTrue(response.isPresent());

                    assertAll(
                            "проверка артефактов БД и ответа клиенту",
                            () -> assertEquals(2, details.size()),
                            () -> assertEquals(studentId, response.get().value().studentId()),
                            () -> assertNotNull(response.get().value().avgScore())
                    );
                });
    }

    @Test
    void example2() {
        // имитация клиентского запроса
        var studentId = "1234";
        clientProducer.send("request-topic", "key", studentId);

        await()
                .pollInterval(Duration.ofSeconds(3))
                .atMost(15, SECONDS)
                .untilAsserted(() -> {
                    var response = Optional.ofNullable(clientConsumerRecords.poll());
                    var details = scoreDetailsPort.findAllByStudentId(studentId);

                    assertTrue(response.isPresent());

                    assertAll(
                            "проверка артефактов БД и ответа клиенту",
                            () -> assertEquals(2, details.size()),
                            () -> assertEquals(studentId, response.get().value().studentId()),
                            () -> assertNotNull(response.get().value().avgScore())
                    );
                });
    }

    @Test
    void example3() {
        // имитация клиентского запроса
        var studentId = "1235";
        clientProducer.send("request-topic", "key", studentId);

        await()
                .pollInterval(Duration.ofSeconds(3))
                .atMost(15, SECONDS)
                .untilAsserted(() -> {
                    var response = Optional.ofNullable(clientConsumerRecords.poll());
                    var details = scoreDetailsPort.findAllByStudentId(studentId);

                    assertTrue(response.isPresent());

                    assertAll(
                            "проверка артефактов БД и ответа клиенту",
                            () -> assertEquals(2, details.size()),
                            () -> assertEquals(studentId, response.get().value().studentId()),
                            () -> assertNotNull(response.get().value().avgScore())
                    );
                });
    }

    @Test
    void example4() {
        // имитация клиентского запроса
        var studentId = "1236";
        clientProducer.send("request-topic", "key", studentId);

        await()
                .pollInterval(Duration.ofSeconds(3))
                .atMost(15, SECONDS)
                .untilAsserted(() -> {
                    var response = Optional.ofNullable(clientConsumerRecords.poll());
                    var details = scoreDetailsPort.findAllByStudentId(studentId);

                    assertTrue(response.isPresent());

                    assertAll(
                            "проверка артефактов БД и ответа клиенту",
                            () -> assertEquals(2, details.size()),
                            () -> assertEquals(studentId, response.get().value().studentId()),
                            () -> assertNotNull(response.get().value().avgScore())
                    );
                });
    }
}
