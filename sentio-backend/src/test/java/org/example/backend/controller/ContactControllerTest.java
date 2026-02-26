package org.example.backend.controller;

import org.example.backend.dto.ContactRequest;
import org.example.backend.service.ContactService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ContactController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@Import(ContactControllerTest.TestBeans.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
@DisplayName("ContactController")
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactService contactService;

    @TestConfiguration
    static class TestBeans {
        @Bean
        ContactService contactService() {
            return mock(ContactService.class);
        }
    }

    @AfterEach
    void resetMocks() {
        reset(contactService);
    }

    @Test
    @DisplayName("send should return 200 with valid request")
    void send_withValidRequest_returns200() throws Exception {
        // Arrange
        doNothing().when(contactService).sendContactMail(any(ContactRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/contact")
                .contentType("application/json")
                .content("""
                        {
                            "mail": "test@example.com",
                            "message": "This is a test message",
                            "name": "John",
                            "surname": "Doe",
                            "reference": "REF123"
                        }
                        """))
                .andExpect(status().isOk());

        verify(contactService, times(1)).sendContactMail(any(ContactRequest.class));
    }

    @Test
    @DisplayName("send should return 400 when email is blank")
    void send_withBlankEmail_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/contact")
                .contentType("application/json")
                .content("""
                        {
                            "mail": "",
                            "message": "This is a test message"
                        }
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contactService);
    }

    @Test
    @DisplayName("send should return 400 when email is null")
    void send_withNullEmail_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/contact")
                .contentType("application/json")
                .content("""
                        {
                            "message": "This is a test message"
                        }
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contactService);
    }

    @Test
    @DisplayName("send should return 400 when message is blank")
    void send_withBlankMessage_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/contact")
                .contentType("application/json")
                .content("""
                        {
                            "mail": "test@example.com",
                            "message": ""
                        }
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contactService);
    }

    @Test
    @DisplayName("send should return 400 when message is null")
    void send_withNullMessage_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/contact")
                .contentType("application/json")
                .content("""
                        {
                            "mail": "test@example.com"
                        }
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contactService);
    }

    @Test
    @DisplayName("send should return 400 when both email and message are blank")
    void send_withBothBlank_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/contact")
                .contentType("application/json")
                .content("""
                        {
                            "mail": "",
                            "message": ""
                        }
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contactService);
    }

    @Test
    @DisplayName("send should return 500 when service throws exception")
    void send_whenServiceThrows_returns500() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Email service unavailable"))
                .when(contactService).sendContactMail(any(ContactRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/contact")
                .contentType("application/json")
                .content("""
                        {
                            "name": "John",
                            "mail": "test@example.com",
                            "message": "This is a test message"
                        }
                        """))
                .andExpect(status().isInternalServerError());

        verify(contactService, times(1)).sendContactMail(any(ContactRequest.class));
    }

    @Test
    @DisplayName("send should accept request with only required fields")
    void send_withOnlyRequiredFields_returns200() throws Exception {
        // Arrange
        doNothing().when(contactService).sendContactMail(any(ContactRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/contact")
                .contentType("application/json")
                .content("""
                        {
                            "name": "Minimal",
                            "mail": "minimal@example.com",
                            "message": "Minimal message"
                        }
                        """))
                .andExpect(status().isOk());

        verify(contactService, times(1)).sendContactMail(any(ContactRequest.class));
    }

    @Test
    @DisplayName("send should accept request with all optional fields")
    void send_withAllFields_returns200() throws Exception {
        // Arrange
        doNothing().when(contactService).sendContactMail(any(ContactRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/contact")
                .contentType("application/json")
                .content("""
                        {
                            "reference": "REF-2024-001",
                            "name": "Jane",
                            "surname": "Smith",
                            "mail": "jane.smith@example.com",
                            "message": "Complete contact form submission"
                        }
                        """))
                .andExpect(status().isOk());

        verify(contactService, times(1)).sendContactMail(any(ContactRequest.class));
    }
}
