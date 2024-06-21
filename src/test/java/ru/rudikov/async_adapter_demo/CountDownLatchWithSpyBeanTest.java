package ru.rudikov.async_adapter_demo;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import ru.rudikov.async_adapter_demo.adapter.primary.kafka.ExampleProducer;
import ru.rudikov.async_adapter_demo.application.port.secondary.ScoreDetailsPort;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

public class CountDownLatchWithSpyBeanTest extends BaseServiceTest {

    @Autowired
    private ScoreDetailsPort scoreDetailsPort;

    @SpyBean
    private ExampleProducer exampleProducer;

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
        doAnswer(invocation -> {
            countDownLatch.countDown();
            return invocation.callRealMethod();
        }).when(exampleProducer).sendMessage(anyString(), anyDouble());

        // имитация клиентского запроса
        var studentId = "123";
        clientProducer.send("request-topic", "key", studentId);

        // блочим тестовый поток до тех пор, пока не вызван countDown()
        countDownLatch.await(15, TimeUnit.SECONDS);

        var details = scoreDetailsPort.findAllByStudentId(studentId);
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Double> doubleArgumentCaptor = ArgumentCaptor.forClass(Double.class);
        Mockito.verify(exampleProducer).sendMessage(stringArgumentCaptor.capture(), doubleArgumentCaptor.capture());
        Double expectedScore = doubleArgumentCaptor.getValue();
        String expectedStudentId = stringArgumentCaptor.getValue();

        assertAll(
                "проверка артефактов БД и ответа клиенту",
                () -> assertEquals(2, details.size()),
                () -> assertEquals(studentId, expectedStudentId),
                () -> assertNotNull(expectedScore)
        );
    }
}
