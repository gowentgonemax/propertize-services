package com.propertize.platform.employecraft.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Explicit Kafka producer configuration.
 *
 * <p>
 * Spring Boot 4.0.x removed Kafka from {@code spring-boot-autoconfigure}, so
 * the
 * {@code KafkaTemplate} must be wired up manually. This class registers a
 * {@link ProducerFactory} and a {@link KafkaTemplate} bean that are picked up
 * by
 * {@link com.propertize.platform.employecraft.event.EmployeeEventPublisher}.
 */
@EnableKafka
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Returns the Kafka producer factory configured for JSON serialisation.
     *
     * @return a {@link DefaultKafkaProducerFactory} instance
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Do not embed type headers — consumers use schema/DTO matching instead
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Returns a {@link KafkaTemplate} backed by the {@link #producerFactory()}.
     *
     * @param producerFactory the configured producer factory
     * @return a ready-to-use {@link KafkaTemplate}
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
