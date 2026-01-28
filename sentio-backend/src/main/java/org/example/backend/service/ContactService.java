package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.ContactRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service to handle contact form submissions.
 * Sends emails via Resend API to the configured recipient.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    private final ResendEmailService emailService;

    @Value("${contact.mail.to:team@syslabs.dev}")
    private String contactTo;

    /**
     * Send a contact form submission as an email.
     */
    public void sendContactMail(ContactRequest request) {
        log.info("Processing contact form submission from: {}", request.getMail());

        String subject = "New contact message: " + safe(request.getReference());

        String text = """
                New contact message from Sentio website:

                Reference: %s
                Name: %s %s
                Email: %s

                Message:
                %s
                """.formatted(
                safe(request.getReference()),
                safe(request.getName()),
                safe(request.getSurname()),
                safe(request.getMail()),
                safe(request.getMessage()));

        // Send email with user's email as reply-to
        emailService.sendEmail(contactTo, subject, text, request.getMail());

        log.info("Contact form email sent successfully to {}", contactTo);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}