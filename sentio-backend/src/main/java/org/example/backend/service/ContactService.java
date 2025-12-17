package org.example.backend.service;

import org.example.backend.dto.ContactRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ContactService {
    private final JavaMailSender mailSender;

    @Value("${contact.mail.to}")
    private String contactTo;

    public ContactService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendContactMail(ContactRequest request) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("SentioSystems@outlook.com");
        msg.setTo(contactTo);
        msg.setSubject("New contact message: " + safe(request.getReference()));

        // set sender as Reply-To if able to
        if (request.getMail() != null && !request.getMail().isBlank()) {
            msg.setReplyTo(request.getMail());
        }

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
                safe(request.getMessage())
        );

        msg.setText(text);

        mailSender.send(msg);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

}