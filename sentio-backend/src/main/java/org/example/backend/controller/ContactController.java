package org.example.backend.controller;

import jakarta.validation.Valid;
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
    public ResponseEntity<Void> send(@Valid @RequestBody ContactRequest request) {
        System.out.println(">>> ContactController reached, mail=" + request.getMail()); //remove if it works out
        try {
            service.sendContactMail(request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
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