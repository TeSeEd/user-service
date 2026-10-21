package com.aston.userservice.kafka;

import com.aston.contracts.event.UserEvent;
import com.aston.contracts.event.UserEventType;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@Testcontainers
class UserEventProducerIntegrationTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:3.9.1");

    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17")
                    .withDatabaseName("user_service_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserEventProducer userEventProducer;

    @Test
    void publish_shouldSendEventToKafka() {
        UserEvent userEvent = new UserEvent(
                UUID.randomUUID(),
                UserEventType.USER_CREATED,
                1L,
                "example@mail.com",
                OffsetDateTime.now(ZoneOffset.UTC),
                UserEvent.CURRENT_SCHEMA_VERSION);

        Map<String, Object> consumerProperties = new HashMap<>();
        consumerProperties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers()
        );
        consumerProperties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        consumerProperties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class
        );
        consumerProperties.put(
                JsonDeserializer.VALUE_DEFAULT_TYPE,
                UserEvent.class.getName()
        );
        consumerProperties.put(
                JsonDeserializer.TRUSTED_PACKAGES,
                "com.aston.contracts.event"
        );
        consumerProperties.put(
                JsonDeserializer.USE_TYPE_INFO_HEADERS,
                false
        );
        consumerProperties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "user-event-producer-test-" + UUID.randomUUID()
        );
        consumerProperties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        try (KafkaConsumer<String, UserEvent> consumer = new KafkaConsumer<>(consumerProperties)) {
            consumer.subscribe(Collections.singletonList("user.events.v1"));
            SendResult<String, UserEvent> result =
                    userEventProducer.publish(userEvent).join();

            assertEquals("user.events.v1", result.getRecordMetadata().topic());

            long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
            ConsumerRecords<String, UserEvent> records;
            do {
                records = consumer.poll(Duration.ofMillis(500));
            } while (records.isEmpty() && System.nanoTime() < deadline);

            assertFalse(records.isEmpty(), "Kafka did not return the published event");

            ConsumerRecord<String, UserEvent> record = records.iterator().next();

            assertEquals("1", record.key());
            assertEquals(userEvent.eventId(), record.value().eventId());
            assertEquals(UserEventType.USER_CREATED, record.value().eventType());
            assertEquals(1L, record.value().userId());
            assertEquals("example@mail.com", record.value().email());
            assertEquals(UserEvent.CURRENT_SCHEMA_VERSION, record.value().schemaVersion());
            assertEquals(userEvent.occurredAt(), record.value().occurredAt());
        }
    }
}