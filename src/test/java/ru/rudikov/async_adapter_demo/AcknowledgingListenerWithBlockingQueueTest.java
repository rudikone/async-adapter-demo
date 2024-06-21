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

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL;

public class AcknowledgingListenerWithBlockingQueueTest extends BaseServiceTest {

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
        clientConsumerProps.put(MAX_POLL_RECORDS_CONFIG, "1"); // ограничиваем размер пачки сообщений
        clientConsumerProps.put(ENABLE_AUTO_COMMIT_CONFIG, "false"); // отключаем автокоммит

        clientConsumerProps.put(MAX_POLL_INTERVAL_MS_CONFIG, "100"); // осознанно неудачное значение, однако лучше делать больше чем ответ сервиса

        var deserializer = new JsonDeserializer<ScoreResult>();
        deserializer.addTrustedPackages("*");
        var clientConsumerFactory = new DefaultKafkaConsumerFactory<>(
                clientConsumerProps,
                new StringDeserializer(),
                deserializer
        );
        var clientConsumerContainerProperties = new ContainerProperties("response-topic");
        clientConsumerContainerProperties.setAckMode(MANUAL); // устанавливаем политику подтверждения MANUAL

        clientConsumer = new KafkaMessageListenerContainer<>(clientConsumerFactory, clientConsumerContainerProperties);
        // задаем поведение клиентского консюмера
        clientConsumer.setupMessageListener(
                // используем AcknowledgingMessageListener
                (AcknowledgingMessageListener<String, ScoreResult>) (data, acknowledgment) -> {
                    try {
                        // поток, обслуживающий клиентский консюмер будет заблокирован, пока в блокирующей очереди
                        // есть хотя бы один необработанный ивент
                        clientConsumerRecords.put(data);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    // коммитим offset сразу же после того, как смогли положить сообщение в блокирующую очередь
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
    void example1() throws InterruptedException {
        // имитация клиентского запроса
        var studentId = "123";
        clientProducer.send("request-topic", "key", studentId);

        // блочим тестовый поток до тех пор, пока клиентский консюмер не получит сообщение
        var response = clientConsumerRecords.poll(15, TimeUnit.SECONDS);
        var details = scoreDetailsPort.findAllByStudentId(studentId);

        assertAll(
                "проверка артефактов БД и ответа клиенту",
                () -> assertEquals(2, details.size()),
                () -> assertEquals(studentId, response.value().studentId()),
                () -> assertNotNull(response.value().avgScore())
        );
    }

    @Test
    void example2() throws InterruptedException {
        // имитация клиентского запроса
        var studentId = "1234";
        clientProducer.send("request-topic", "key", studentId);

        // блочим тестовый поток до тех пор, пока клиентский консюмер не получит сообщение
        var response = clientConsumerRecords.poll(15, TimeUnit.SECONDS);
        var details = scoreDetailsPort.findAllByStudentId(studentId);

        assertAll(
                "проверка артефактов БД и ответа клиенту",
                () -> assertEquals(2, details.size()),
                () -> assertEquals(studentId, response.value().studentId()),
                () -> assertNotNull(response.value().avgScore())
        );
    }

    @Test
    void example3() throws InterruptedException {
        // имитация клиентского запроса
        var studentId = "1235";
        clientProducer.send("request-topic", "key", studentId);

        // блочим тестовый поток до тех пор, пока клиентский консюмер не получит сообщение
        var response = clientConsumerRecords.poll(15, TimeUnit.SECONDS);
        var details = scoreDetailsPort.findAllByStudentId(studentId);

        assertAll(
                "проверка артефактов БД и ответа клиенту",
                () -> assertEquals(2, details.size()),
                () -> assertEquals(studentId, response.value().studentId()),
                () -> assertNotNull(response.value().avgScore())
        );
    }

    @Test
    void example4() throws InterruptedException {
        // имитация клиентского запроса
        var studentId = "1236";
        clientProducer.send("request-topic", "key", studentId);

        // блочим тестовый поток до тех пор, пока клиентский консюмер не получит сообщение
        var response = clientConsumerRecords.poll(15, TimeUnit.SECONDS);
        var details = scoreDetailsPort.findAllByStudentId(studentId);

        assertAll(
                "проверка артефактов БД и ответа клиенту",
                () -> assertEquals(2, details.size()),
                () -> assertEquals(studentId, response.value().studentId()),
                () -> assertNotNull(response.value().avgScore())
        );
    }
}
