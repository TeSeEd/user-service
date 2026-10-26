package com.aston.notificationservice.kafka;

import com.aston.contracts.event.UserEvent;
import com.aston.notificationservice.service.UserEventService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventConsumer {
    private final UserEventService userEventService;

    public UserEventConsumer(UserEventService userEventService) {
        this.userEventService = userEventService;
    }

    @KafkaListener(
            topics = "user.events.v1",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(UserEvent event) {
        userEventService.process(event);
    }
}