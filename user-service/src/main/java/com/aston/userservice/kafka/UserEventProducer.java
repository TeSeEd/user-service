package com.aston.userservice.kafka;

import com.aston.contracts.event.UserEvent;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class UserEventProducer {
    private static final String TOPIC = "user.events.v1";
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;
    private static final Logger log = LoggerFactory.getLogger(UserEventProducer.class);

    public UserEventProducer(KafkaTemplate<String, UserEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, UserEvent>> publish(UserEvent userEvent) {
        CompletableFuture<SendResult<String, UserEvent>> result = kafkaTemplate.send(TOPIC, userEvent.userId().toString(), userEvent);
        return result.whenComplete((r, e) -> {
            if (e != null) {
                log.error("Failed to publish user event", e);
            } else {
                RecordMetadata metadata = r.getRecordMetadata();
                log.info(
                        "User event published: eventId={}, partition={}, offset={}",
                        userEvent.eventId(),
                        metadata.partition(),
                        metadata.offset()
                );
            }
        });
    }
}
