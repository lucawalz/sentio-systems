package org.example.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContactRequest {
    @NotNull(message = "Reference is required")
    @NotBlank(message = "Reference cannot be blank")
    @Size(max = 255, message = "Reference must be at most 255 characters")
    private String reference;

    @NotNull(message = "Name is required")
    @NotBlank(message = "Name cannot be blank")
    @Size(max = 255, message = "Name must be at most 255 characters")
    private String name;

    @NotNull(message = "Surname is required")
    @NotBlank(message = "Surname cannot be blank")
    @Size(max = 255, message = "Surname must be at most 255 characters")
    private String surname;

    @NotNull(message = "Email is required")
    @Email(message = "Email must be valid")
    private String mail;

    @NotNull(message = "Message is required")
    @NotBlank(message = "Message cannot be blank")
    @Size(min = 10, max = 5000, message = "Message must be between 10 and 5000 characters")
    private String message;
}