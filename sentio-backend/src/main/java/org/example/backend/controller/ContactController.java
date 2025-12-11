package org.example.backend.controller;

import org.example.backend.dto.ContactRequest;
import org.example.backend.service.ContactService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
@CrossOrigin // check Port !!!! //needed for check if frontend has different port than backend / api
public class ContactController {

    private final ContactService service;

    public ContactController(ContactService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> send(@RequestBody ContactRequest request) {
        // easy server validation (add to frontend)
        if (isBlank(request.getMail()) || isBlank(request.getMessage())) {
            return ResponseEntity.badRequest().build();
        }

        service.sendContactMail(request);
        return ResponseEntity.ok().build();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}