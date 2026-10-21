package com.aston.userservice.kafka;

import com.aston.contracts.event.UserEvent;
import com.aston.contracts.event.UserEventType;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class UserEventProducerTest {
    @Mock
    private KafkaTemplate<String, UserEvent> kafkaTemplate;
    @InjectMocks
    private UserEventProducer userEventProducer;

    private final UserEvent userEvent = new UserEvent(UUID.randomUUID(), UserEventType.USER_CREATED, 1L, "example@mail.com", OffsetDateTime.now(ZoneOffset.UTC), UserEvent.CURRENT_SCHEMA_VERSION);

    @Test
    void publish_shouldSendEventWithCorrectTopicKeyAndValue() {
        CompletableFuture<SendResult<String, UserEvent>> future = new CompletableFuture<>();

        when(kafkaTemplate.send("user.events.v1", "1", userEvent)).thenReturn(future);

        userEventProducer.publish(userEvent);

        verify(kafkaTemplate).send("user.events.v1", "1", userEvent);
    }

    @Test
    void publish_shouldHandleSuccessfulSend() {
        SendResult<String, UserEvent> result = mock(SendResult.class);
        RecordMetadata metadata = mock(RecordMetadata.class);

        when(result.getRecordMetadata()).thenReturn(metadata);

        CompletableFuture<SendResult<String, UserEvent>> future = CompletableFuture.completedFuture(result);

        when(kafkaTemplate.send("user.events.v1", "1", userEvent)).thenReturn(future);

        userEventProducer.publish(userEvent);

        verify(result).getRecordMetadata();
        verify(metadata).partition();
        verify(metadata).offset();
    }

    @Test
    void publish_shouldReturnFailedFuture_whenKafkaSendFails() {
        RuntimeException exception = new RuntimeException("Kafka unavailable");
        CompletableFuture<SendResult<String, UserEvent>> future = CompletableFuture.failedFuture(exception);

        when(kafkaTemplate.send("user.events.v1", "1", userEvent)).thenReturn(future);

        CompletableFuture<SendResult<String, UserEvent>> actualFuture = userEventProducer.publish(userEvent);

        assertThrows(CompletionException.class, actualFuture::join);
    }
}
