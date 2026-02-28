package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.ContactRequest;
import org.example.backend.service.ContactService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
@Slf4j
public class ContactController {
    /**
     * REST controller for handling contact form submissions.
     * Receives contact requests from the frontend and triggers email notifications.
     * Validates input and delegates mail sending to ContactService.
     */

    private final ContactService service;

    public ContactController(ContactService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> send(@Valid @RequestBody ContactRequest request) {
        log.info("Processing contact request for {}", request.getMail());
        try {
            service.sendContactMail(request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to process contact request", e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/test")
    public void test() {
        ContactRequest r = new ContactRequest();
        r.setMail("test@test.de");
        r.setMessage("Hello");
        service.sendContactMail(r);
    }
}