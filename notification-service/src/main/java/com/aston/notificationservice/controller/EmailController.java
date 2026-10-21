package com.aston.notificationservice.controller;

import com.aston.notificationservice.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/email")
    public ResponseEntity<Void> sendEmail(
            @Valid @RequestBody SendEmailRequest request
    ) {
        emailService.send(
                request.to(),
                request.subject(),
                request.body()
        );

        return ResponseEntity.accepted().build();
    }
}