package com.aston.notificationservice.service;

import com.aston.notificationservice.model.EventType;

import java.util.UUID;

public record ClaimedNotification(
        Long id,
        UUID claimToken,
        String recipientEmail,
        EventType eventType
) {
}