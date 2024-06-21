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
import static org.apache.kafka.clients.consumer.ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL;

public class AcknowledgingListenerWithBlockingQueueTest extends BaseServiceTest {

    // имитация клиентского продюсера
    private static KafkaTemplate<String, String> clientProducer;

    // имитация клиентского консюмера
    private static KafkaMessageListenerContainer<String, ScoreResult> clientConsumer;
    // блокирующая очередь с размером 1, чтобы гарантировать последовательную обработку сообщений во всех тестах
    private static final BlockingQueue<ConsumerRecord<String, ScoreResult>> CLIENT_CONSUMER_RECORDS =
            new LinkedBlockingQueue<>(1);

    @Autowired
    private ScoreDetailsPort scoreDetailsPort;

    @BeforeAll
    static void setUp() {
        // создаем имитацию клиентского продюсера
        final var clientProducerProps = KafkaTestUtils.producerProps(KAFKA_CONTAINER.getBootstrapServers());
        clientProducerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        clientProducerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        clientProducer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(clientProducerProps));

        // создаем имитацию клиентского консюмера
        final var clientConsumerProps = new HashMap<String, Object>();
        clientConsumerProps.put(BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());
        clientConsumerProps.put(GROUP_ID_CONFIG, "consumer");
        clientConsumerProps.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        clientConsumerProps.put(ALLOW_AUTO_CREATE_TOPICS_CONFIG, "false");
        clientConsumerProps.put(MAX_POLL_RECORDS_CONFIG, "1"); // ограничиваем размер пачки сообщений
        clientConsumerProps.put(ENABLE_AUTO_COMMIT_CONFIG, "false"); // отключаем автокоммит

        clientConsumerProps.put(MAX_POLL_INTERVAL_MS_CONFIG, "100"); // осознанно неудачное значение, однако лучше делать больше чем ответ сервиса

        final var deserializer = new JsonDeserializer<ScoreResult>();
        deserializer.addTrustedPackages("*");
        final var clientConsumerFactory = new DefaultKafkaConsumerFactory<>(
                clientConsumerProps,
                new StringDeserializer(),
                deserializer
        );
        final var clientConsumerContainerProperties = new ContainerProperties("response-topic");
        clientConsumerContainerProperties.setAckMode(MANUAL); // устанавливаем политику подтверждения MANUAL

        clientConsumer = new KafkaMessageListenerContainer<>(clientConsumerFactory, clientConsumerContainerProperties);
        // задаем поведение клиентского консюмера
        clientConsumer.setupMessageListener(
                // используем AcknowledgingMessageListener
                (AcknowledgingMessageListener<String, ScoreResult>) (data, acknowledgment) -> {
                    try {
                        // поток, обслуживающий клиентский консюмер будет заблокирован, пока в блокирующей очереди
                        // есть хотя бы один необработанный ивент
                        CLIENT_CONSUMER_RECORDS.put(data);
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
        final var studentId = "123";
        clientProducer.send("request-topic", "key", studentId);

        // блочим тестовый поток до тех пор, пока клиентский консюмер не получит сообщение
        final var response = CLIENT_CONSUMER_RECORDS.poll(15, TimeUnit.SECONDS);
        final var details = scoreDetailsPort.findAllByStudentId(studentId);

        assertThat(details).as("проверка артефактов БД").hasSize(2);
        assertThat(response.value())
                .as("проверка ответа клиенту")
                .satisfies(value -> {
                    assertThat(value.studentId()).isEqualTo(studentId);
                    assertThat(value.avgScore()).isNotNull();
                });
    }

    @Test
    void example2() throws InterruptedException {
        // имитация клиентского запроса
        final var studentId = "1234";
        clientProducer.send("request-topic", "key", studentId);

        // блочим тестовый поток до тех пор, пока клиентский консюмер не получит сообщение
        final var response = CLIENT_CONSUMER_RECORDS.poll(15, TimeUnit.SECONDS);
        final var details = scoreDetailsPort.findAllByStudentId(studentId);

        assertThat(details).as("проверка артефактов БД").hasSize(2);
        assertThat(response.value())
                .as("проверка ответа клиенту")
                .satisfies(value -> {
                    assertThat(value.studentId()).isEqualTo(studentId);
                    assertThat(value.avgScore()).isNotNull();
                });
    }

    @Test
    void example3() throws InterruptedException {
        // имитация клиентского запроса
        final var studentId = "1235";
        clientProducer.send("request-topic", "key", studentId);

        // блочим тестовый поток до тех пор, пока клиентский консюмер не получит сообщение
        final var response = CLIENT_CONSUMER_RECORDS.poll(15, TimeUnit.SECONDS);
        final var details = scoreDetailsPort.findAllByStudentId(studentId);

        assertThat(details).as("проверка артефактов БД").hasSize(2);
        assertThat(response.value())
                .as("проверка ответа клиенту")
                .satisfies(value -> {
                    assertThat(value.studentId()).isEqualTo(studentId);
                    assertThat(value.avgScore()).isNotNull();
                });
    }

    @Test
    void example4() throws InterruptedException {
        // имитация клиентского запроса
        final var studentId = "1236";
        clientProducer.send("request-topic", "key", studentId);

        // блочим тестовый поток до тех пор, пока клиентский консюмер не получит сообщение
        final var response = CLIENT_CONSUMER_RECORDS.poll(15, TimeUnit.SECONDS);
        final var details = scoreDetailsPort.findAllByStudentId(studentId);

        assertThat(details).as("проверка артефактов БД").hasSize(2);
        assertThat(response.value())
                .as("проверка ответа клиенту")
                .satisfies(value -> {
                    assertThat(value.studentId()).isEqualTo(studentId);
                    assertThat(value.avgScore()).isNotNull();
                });
    }
}
