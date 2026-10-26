package com.aston.notificationservice.service;

import com.aston.contracts.event.UserEvent;
import com.aston.notificationservice.entity.Notification;
import com.aston.notificationservice.model.EventType;
import com.aston.notificationservice.repository.NotificationRepository;
import com.aston.notificationservice.repository.ProcessedEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserEventService {
    private final NotificationRepository notificationRepository;
    private final ProcessedEventRepository processedEventRepository;

    public UserEventService(
            NotificationRepository notificationRepository,
            ProcessedEventRepository processedEventRepository) {
        this.notificationRepository = notificationRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public void process(UserEvent userEvent) {
        if (!processedEventRepository.registerIfAbsent(userEvent.eventId())) {
            return;
        }
        notificationRepository.save(
                new Notification(
                        userEvent.eventId(),
                        userEvent.email(),
                        EventType.valueOf(userEvent.eventType().name())
                )
        );
    }
}
