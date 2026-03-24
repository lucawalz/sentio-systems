package dev.syslabs.sentio.service;

import dev.syslabs.sentio.dto.ContactRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ContactService}.
 * Tests contact form submission, validation, and email sending.
 */
@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ResendEmailService emailService;

    @InjectMocks
    private ContactService contactService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contactService, "contactTo", "team@syslabs.dev");
    }

    @Nested
    @DisplayName("sendContactMail")
    class SendContactMailTests {

        @Test
        @DisplayName("should send email with all fields populated")
        void shouldSendEmailWithAllFields() {
            // Given
            ContactRequest request = new ContactRequest();
            request.setReference("Product Inquiry");
            request.setName("John");
            request.setSurname("Doe");
            request.setMail("john.doe@example.com");
            request.setMessage("I am interested in your product.");

            // When
            contactService.sendContactMail(request);

            // Then
            ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> replyToCaptor = ArgumentCaptor.forClass(String.class);

            verify(emailService).sendEmail(
                    toCaptor.capture(),
                    subjectCaptor.capture(),
                    textCaptor.capture(),
                    replyToCaptor.capture()
            );

            assertThat(toCaptor.getValue()).isEqualTo("team@syslabs.dev");
            assertThat(subjectCaptor.getValue()).isEqualTo("New contact message: Product Inquiry");
            assertThat(textCaptor.getValue())
                    .contains("Product Inquiry")
                    .contains("John")
                    .contains("Doe")
                    .contains("john.doe@example.com")
                    .contains("I am interested in your product.");
            assertThat(replyToCaptor.getValue()).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("should use custom contact recipient email")
        void shouldUseCustomContactRecipient() {
            // Given
            ReflectionTestUtils.setField(contactService, "contactTo", "custom@example.com");
            ContactRequest request = new ContactRequest();
            request.setReference("Test");
            request.setName("Test");
            request.setSurname("User");
            request.setMail("test@example.com");
            request.setMessage("Test message");

            // When
            contactService.sendContactMail(request);

            // Then
            verify(emailService).sendEmail(
                    eq("custom@example.com"),
                    anyString(),
                    anyString(),
                    anyString()
            );
        }

        @Test
        @DisplayName("should include reference in subject line")
        void shouldIncludeReferenceInSubject() {
            // Given
            ContactRequest request = new ContactRequest();
            request.setReference("Urgent Support Request");
            request.setName("Jane");
            request.setSurname("Smith");
            request.setMail("jane@example.com");
            request.setMessage("Need help with installation");

            // When
            contactService.sendContactMail(request);

            // Then
            verify(emailService).sendEmail(
                    anyString(),
                    eq("New contact message: Urgent Support Request"),
                    anyString(),
                    anyString()
            );
        }

        @Test
        @DisplayName("should set reply-to as sender email")
        void shouldSetReplyToAsSenderEmail() {
            // Given
            ContactRequest request = new ContactRequest();
            request.setReference("Question");
            request.setName("Bob");
            request.setSurname("Johnson");
            request.setMail("bob.johnson@example.com");
            request.setMessage("Quick question");

            // When
            contactService.sendContactMail(request);

            // Then
            verify(emailService).sendEmail(
                    anyString(),
                    anyString(),
                    anyString(),
                    eq("bob.johnson@example.com")
            );
        }
    }

    @Nested
    @DisplayName("validation and null handling")
    class ValidationTests {

        @Test
        @DisplayName("should handle null reference field")
        void shouldHandleNullReference() {
            // Given
            ContactRequest request = new ContactRequest();
            request.setReference(null);
            request.setName("John");
            request.setSurname("Doe");
            request.setMail("john@example.com");
            request.setMessage("Test message");

            // When/Then
            assertThatCode(() -> contactService.sendContactMail(request))
                    .doesNotThrowAnyException();

            ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);

            verify(emailService).sendEmail(
                    anyString(),
                    subjectCaptor.capture(),
                    textCaptor.capture(),
                    anyString()
            );

            assertThat(subjectCaptor.getValue()).isEqualTo("New contact message: ");
            assertThat(textCaptor.getValue()).contains("Reference: \n");
        }

        @Test
        @DisplayName("should handle null name field")
        void shouldHandleNullName() {
            // Given
            ContactRequest request = new ContactRequest();
            request.setReference("Test");
            request.setName(null);
            request.setSurname("Doe");
            request.setMail("john@example.com");
            request.setMessage("Test message");

            // When/Then
            assertThatCode(() -> contactService.sendContactMail(request))
                    .doesNotThrowAnyException();

            ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailService).sendEmail(
                    anyString(),
                    anyString(),
                    textCaptor.capture(),
                    anyString()
            );

            assertThat(textCaptor.getValue()).contains("Name:  Doe");
        }

        @Test
        @DisplayName("should handle null surname field")
        void shouldHandleNullSurname() {
            // Given
            ContactRequest request = new ContactRequest();
            request.setReference("Test");
            request.setName("John");
            request.setSurname(null);
            request.setMail("john@example.com");
            request.setMessage("Test message");

            // When/Then
            assertThatCode(() -> contactService.sendContactMail(request))
                    .doesNotThrowAnyException();

            ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailService).sendEmail(
                    anyString(),
                    anyString(),
                    textCaptor.capture(),
                    anyString()
            );

            assertThat(textCaptor.getValue()).contains("Name: John ");
        }

        @Test
        @DisplayName("should handle null message field")
        void shouldHandleNullMessage() {
            // Given
            ContactRequest request = new ContactRequest();
            request.setReference("Test");
            request.setName("John");
            request.setSurname("Doe");
            request.setMail("john@example.com");
            request.setMessage(null);

            // When/Then
            assertThatCode(() -> contactService.sendContactMail(request))
                    .doesNotThrowAnyException();

            ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailService).sendEmail(
                    anyString(),
                    anyString(),
                    textCaptor.capture(),
                    anyString()
            );

            assertThat(textCaptor.getValue()).contains("Message:\n");
        }

        @Test
        @DisplayName("should handle all null fields")
        void shouldHandleAllNullFields() {
            // Given
            ContactRequest request = new ContactRequest();
            request.setReference(null);
            request.setName(null);
            request.setSurname(null);
            request.setMail(null);
            request.setMessage(null);

            // When/Then
            assertThatCode(() -> contactService.sendContactMail(request))
                    .doesNotThrowAnyException();

            verify(emailService).sendEmail(
                    eq("team@syslabs.dev"),
                    anyString(),
                    anyString(),
                    isNull()
            );
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle email service failure gracefully")
        void shouldHandleEmailServiceFailure() {
            // Given
            ContactRequest request = new ContactRequest();
            request.setReference("Test");
            request.setName("John");
            request.setSurname("Doe");
            request.setMail("john@example.com");
            request.setMessage("Test message");

            doThrow(new RuntimeException("Email service is down"))
                    .when(emailService).sendEmail(anyString(), anyString(), anyString(), anyString());

            // When/Then - should throw exception (no try-catch in service)
            assertThatCode(() -> contactService.sendContactMail(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Email service is down");

            verify(emailService).sendEmail(
                    eq("team@syslabs.dev"),
                    anyString(),
                    anyString(),
                    eq("john@example.com")
            );
        }

        @Test
        @DisplayName("should handle email service timeout")
        void shouldHandleEmailServiceTimeout() {
            // Given
            ContactRequest request = new ContactRequest();
            request.setReference("Timeout Test");
            request.setName("Alice");
            request.setSurname("Wonder");
            request.setMail("alice@example.com");
            request.setMessage("This will timeout");

            doThrow(new RuntimeException("Connection timeout"))
                    .when(emailService).sendEmail(anyString(), anyString(), anyString(), anyString());

            // When/Then
            assertThatCode(() -> contactService.sendContactMail(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Connection timeout");
        }
    }
}
