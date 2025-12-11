package org.example.backend.dto;

import lombok.Data;

@Data
public class ContactRequest {
    private String reference;
    private String name;
    private String surname;
    private String mail;
    private String message;
}