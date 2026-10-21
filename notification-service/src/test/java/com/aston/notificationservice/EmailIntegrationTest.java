
package com.aston.notificationservice;

import com.aston.notificationservice.controller.EmailController;
import com.aston.notificationservice.model.EventType;
import com.aston.notificationservice.service.EmailService;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmailIntegrationTest {

    private GreenMail greenMail;
    private EmailService emailService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        greenMail = new GreenMail(
                ServerSetupTest.SMTP.dynamicPort()
        );
        greenMail.start();

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("127.0.0.1");
        mailSender.setPort(greenMail.getSmtp().getPort());
        mailSender.getJavaMailProperties()
                .put("mail.smtp.auth", "false");

        emailService = new EmailService(
                mailSender,
                "noreply@astonhub.test"
        );

        mockMvc = MockMvcBuilders.standaloneSetup(
                new EmailController(emailService)
        ).build();
    }

    @AfterEach
    void tearDown() {
        if (greenMail != null) {
            greenMail.stop();
        }
    }

    @Test
    void shouldSendEmailWhenUserCreated() throws Exception {
        emailService.sendUserNotification(
                "created@example.com",
                EventType.USER_CREATED
        );

        MimeMessage message = getSingleEmail();

        assertEquals(
                "created@example.com",
                message.getAllRecipients()[0].toString()
        );

        assertEquals(
                "Добро пожаловать в AstonHub!",
                message.getSubject()
        );

        assertEquals(
                "Здравствуйте! Ваш аккаунт на сайте AstonHub был успешно создан.",
                message.getContent().toString()
        );
    }

    @Test
    void shouldSendEmailWhenUserDeleted() throws Exception {
        emailService.sendUserNotification(
                "deleted@example.com",
                EventType.USER_DELETED
        );

        MimeMessage message = getSingleEmail();

        assertEquals(
                "deleted@example.com",
                message.getAllRecipients()[0].toString()
        );

        assertEquals(
                "Удаление аккаунта в AstonHub",
                message.getSubject()
        );

        assertEquals(
                "Здравствуйте! Ваш аккаунт на сайте AstonHub был удалён.",
                message.getContent().toString()
        );
    }

    @Test
    void shouldSendEmailThroughRestApi() throws Exception {
        String json = """
                {
                    "to": "rest@example.com",
                    "subject": "REST API test",
                    "body": "Hello from AstonHub"
                }
                """;

        mockMvc.perform(
                post("/api/notifications/email")
                        .contentType("application/json")
                        .content(json)
        ).andExpect(status().isAccepted());

        MimeMessage message = getSingleEmail();

        assertEquals(
                "rest@example.com",
                message.getAllRecipients()[0].toString()
        );

        assertEquals(
                "REST API test",
                message.getSubject()
        );

        assertEquals(
                "Hello from AstonHub",
                message.getContent().toString()
        );
    }

    private MimeMessage getSingleEmail() {
        assertTrue(
                greenMail.waitForIncomingEmail(5000, 1),
                "SMTP did not receive an email"
        );

        MimeMessage[] messages = greenMail.getReceivedMessages();

        assertEquals(1, messages.length);
        return messages[0];
    }
}
