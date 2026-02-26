package org.example.backend.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Jakarta Bean Validation constraints on request DTOs.
 * Uses the Jakarta Validator directly (no Spring context required).
 */
class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Nested
    @DisplayName("ContactRequest validation")
    class ContactRequestTests {

        @Test
        @DisplayName("valid request has no violations")
        void validRequest() {
            ContactRequest request = new ContactRequest();
            request.setName("John");
            request.setSurname("Doe");
            request.setMail("john@example.com");
            request.setMessage("Hello there!");

            Set<ConstraintViolation<ContactRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("blank name is rejected")
        void blankNameRejected() {
            ContactRequest request = new ContactRequest();
            request.setName("");
            request.setMail("john@example.com");
            request.setMessage("Hello");

            Set<ConstraintViolation<ContactRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }

        @Test
        @DisplayName("blank email is rejected")
        void blankEmailRejected() {
            ContactRequest request = new ContactRequest();
            request.setName("John");
            request.setMail("");
            request.setMessage("Hello");

            Set<ConstraintViolation<ContactRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("mail"));
        }

        @Test
        @DisplayName("invalid email format is rejected")
        void invalidEmailRejected() {
            ContactRequest request = new ContactRequest();
            request.setName("John");
            request.setMail("not-an-email");
            request.setMessage("Hello");

            Set<ConstraintViolation<ContactRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("mail"));
        }

        @Test
        @DisplayName("blank message is rejected")
        void blankMessageRejected() {
            ContactRequest request = new ContactRequest();
            request.setName("John");
            request.setMail("john@example.com");
            request.setMessage("");

            Set<ConstraintViolation<ContactRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("message"));
        }

        @Test
        @DisplayName("name exceeding 100 chars is rejected")
        void nameTooLong() {
            ContactRequest request = new ContactRequest();
            request.setName("A".repeat(101));
            request.setMail("john@example.com");
            request.setMessage("Hello");

            Set<ConstraintViolation<ContactRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }

        @Test
        @DisplayName("message exceeding 2000 chars is rejected")
        void messageTooLong() {
            ContactRequest request = new ContactRequest();
            request.setName("John");
            request.setMail("john@example.com");
            request.setMessage("A".repeat(2001));

            Set<ConstraintViolation<ContactRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("message"));
        }
    }

    @Nested
    @DisplayName("DevicePairRequest validation")
    class DevicePairRequestTests {

        @Test
        @DisplayName("valid request has no violations")
        void validRequest() {
            DevicePairRequest request = DevicePairRequest.builder()
                    .deviceId("device-123")
                    .pairingCode("ABCD-1234")
                    .build();

            Set<ConstraintViolation<DevicePairRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("blank deviceId is rejected")
        void blankDeviceId() {
            DevicePairRequest request = DevicePairRequest.builder()
                    .deviceId("")
                    .pairingCode("ABCD-1234")
                    .build();

            Set<ConstraintViolation<DevicePairRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("deviceId"));
        }

        @Test
        @DisplayName("invalid pairing code format is rejected")
        void invalidPairingCodeFormat() {
            DevicePairRequest request = DevicePairRequest.builder()
                    .deviceId("device-123")
                    .pairingCode("invalid")
                    .build();

            Set<ConstraintViolation<DevicePairRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("pairingCode"));
        }

        @Test
        @DisplayName("lowercase pairing code is rejected")
        void lowercasePairingCode() {
            DevicePairRequest request = DevicePairRequest.builder()
                    .deviceId("device-123")
                    .pairingCode("abcd-1234")
                    .build();

            Set<ConstraintViolation<DevicePairRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("pairingCode"));
        }

        @Test
        @DisplayName("valid uppercase alphanumeric pairing code passes")
        void validPairingCode() {
            DevicePairRequest request = DevicePairRequest.builder()
                    .deviceId("device-123")
                    .pairingCode("A1B2-C3D4")
                    .build();

            Set<ConstraintViolation<DevicePairRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("AuthDTOs.RegisterRequest validation")
    class RegisterRequestTests {

        @Test
        @DisplayName("valid request has no violations")
        void validRequest() {
            AuthDTOs.RegisterRequest request = new AuthDTOs.RegisterRequest();
            request.setUsername("johndoe");
            request.setPassword("securePass1");
            request.setEmail("john@example.com");
            request.setFirstName("John");
            request.setLastName("Doe");

            Set<ConstraintViolation<AuthDTOs.RegisterRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("username shorter than 3 chars is rejected")
        void usernameTooShort() {
            AuthDTOs.RegisterRequest request = new AuthDTOs.RegisterRequest();
            request.setUsername("ab");
            request.setPassword("securePass1");
            request.setEmail("john@example.com");
            request.setFirstName("John");
            request.setLastName("Doe");

            Set<ConstraintViolation<AuthDTOs.RegisterRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        }

        @Test
        @DisplayName("username longer than 50 chars is rejected")
        void usernameTooLong() {
            AuthDTOs.RegisterRequest request = new AuthDTOs.RegisterRequest();
            request.setUsername("A".repeat(51));
            request.setPassword("securePass1");
            request.setEmail("john@example.com");
            request.setFirstName("John");
            request.setLastName("Doe");

            Set<ConstraintViolation<AuthDTOs.RegisterRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        }

        @Test
        @DisplayName("password shorter than 8 chars is rejected")
        void passwordTooShort() {
            AuthDTOs.RegisterRequest request = new AuthDTOs.RegisterRequest();
            request.setUsername("johndoe");
            request.setPassword("short");
            request.setEmail("john@example.com");
            request.setFirstName("John");
            request.setLastName("Doe");

            Set<ConstraintViolation<AuthDTOs.RegisterRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @Test
        @DisplayName("blank email is rejected")
        void blankEmail() {
            AuthDTOs.RegisterRequest request = new AuthDTOs.RegisterRequest();
            request.setUsername("johndoe");
            request.setPassword("securePass1");
            request.setEmail("");
            request.setFirstName("John");
            request.setLastName("Doe");

            Set<ConstraintViolation<AuthDTOs.RegisterRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("invalid email format is rejected")
        void invalidEmail() {
            AuthDTOs.RegisterRequest request = new AuthDTOs.RegisterRequest();
            request.setUsername("johndoe");
            request.setPassword("securePass1");
            request.setEmail("not-an-email");
            request.setFirstName("John");
            request.setLastName("Doe");

            Set<ConstraintViolation<AuthDTOs.RegisterRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }
    }

    @Nested
    @DisplayName("AuthDTOs.LoginRequest validation")
    class LoginRequestTests {

        @Test
        @DisplayName("valid request has no violations")
        void validRequest() {
            AuthDTOs.LoginRequest request = new AuthDTOs.LoginRequest();
            request.setUsername("johndoe");
            request.setPassword("securePass1");

            Set<ConstraintViolation<AuthDTOs.LoginRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("blank username is rejected")
        void blankUsername() {
            AuthDTOs.LoginRequest request = new AuthDTOs.LoginRequest();
            request.setUsername("");
            request.setPassword("securePass1");

            Set<ConstraintViolation<AuthDTOs.LoginRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        }

        @Test
        @DisplayName("blank password is rejected")
        void blankPassword() {
            AuthDTOs.LoginRequest request = new AuthDTOs.LoginRequest();
            request.setUsername("johndoe");
            request.setPassword("");

            Set<ConstraintViolation<AuthDTOs.LoginRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }
    }
}
