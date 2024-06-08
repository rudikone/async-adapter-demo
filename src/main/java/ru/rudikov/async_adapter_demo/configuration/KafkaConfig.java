package ru.rudikov.async_adapter_demo.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public NewTopic requestTopic() {
        return TopicBuilder.name("request-topic").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic responseTopic() {
        return TopicBuilder.name("response-topic").partitions(3).replicas(1).build();
    }
}
