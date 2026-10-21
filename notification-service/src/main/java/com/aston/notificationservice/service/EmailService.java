package com.aston.notificationservice.service;

import com.aston.notificationservice.model.EventType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String senderAddress;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String senderAddress
    ) {
        this.mailSender = mailSender;
        this.senderAddress = senderAddress;
    }

    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    public void sendUserNotification(
            String email,
            EventType eventType
    ) {
        switch (eventType) {
            case USER_CREATED -> send(
                    email,
                    "Добро пожаловать в AstonHub!",
                    "Здравствуйте! Ваш аккаунт на сайте AstonHub был успешно создан."
            );

            case USER_DELETED -> send(
                    email,
                    "Удаление аккаунта в AstonHub",
                    "Здравствуйте! Ваш аккаунт на сайте AstonHub был удалён."
            );
        }
    }
}