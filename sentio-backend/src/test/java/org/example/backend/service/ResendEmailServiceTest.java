package org.example.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResendEmailServiceTest {

    private static final String TEST_API_KEY = "re_test_api_key_12345";
    private static final String TEST_FROM_EMAIL = "noreply@syslabs.dev";
    private static final String TEST_RECIPIENT = "user@example.com";
    private static final String TEST_SUBJECT = "Test Subject";
    private static final String TEST_BODY = "Test email body";
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private ResendEmailService emailService;
    private RestTemplate mockRestTemplate;

    @BeforeEach
    void setUp() {
        emailService = new ResendEmailService();
        mockRestTemplate = mock(RestTemplate.class);

        ReflectionTestUtils.setField(emailService, "apiKey", TEST_API_KEY);
        ReflectionTestUtils.setField(emailService, "fromEmail", TEST_FROM_EMAIL);
        ReflectionTestUtils.setField(emailService, "restTemplate", mockRestTemplate);
    }

    // ========== Email Sending Tests (7 tests) ==========

    @Nested
    @DisplayName("Email Sending")
    class EmailSendingTests {

        @Test
        @DisplayName("should send plain text email successfully")
        void shouldSendPlainTextEmail() {
            when(mockRestTemplate.postForEntity(
                    eq(RESEND_API_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>("{\"id\":\"123\"}", HttpStatus.OK));

            emailService.sendEmail(TEST_RECIPIENT, TEST_SUBJECT, TEST_BODY, null);

            ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(mockRestTemplate).postForEntity(eq(RESEND_API_URL), captor.capture(), eq(String.class));

            HttpEntity<Map<String, Object>> request = captor.getValue();
            Map<String, Object> body = request.getBody();

            assertThat(body).isNotNull();
            assertThat(body.get("from")).isEqualTo(TEST_FROM_EMAIL);
            assertThat(body.get("to")).isEqualTo(List.of(TEST_RECIPIENT));
            assertThat(body.get("subject")).isEqualTo(TEST_SUBJECT);
            assertThat(body.get("text")).isEqualTo(TEST_BODY);
            assertThat(body).doesNotContainKey("reply_to");

            HttpHeaders headers = request.getHeaders();
            assertThat(headers.getFirst("Authorization")).isEqualTo("Bearer " + TEST_API_KEY);
            assertThat(headers.getContentType().toString()).contains("application/json");
        }

        @Test
        @DisplayName("should send HTML email successfully")
        void shouldSendHtmlEmail() {
            String htmlContent = "<h1>Test HTML</h1><p>Content</p>";

            when(mockRestTemplate.postForEntity(
                    eq(RESEND_API_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>("{\"id\":\"123\"}", HttpStatus.OK));

            emailService.sendHtmlEmail(TEST_RECIPIENT, TEST_SUBJECT, htmlContent, null);

            ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(mockRestTemplate).postForEntity(eq(RESEND_API_URL), captor.capture(), eq(String.class));

            Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("html")).isEqualTo(htmlContent);
            assertThat(body).doesNotContainKey("text");
        }

        @Test
        @DisplayName("should send email with reply-to address")
        void shouldSendEmailWithReplyTo() {
            String replyTo = "support@example.com";

            when(mockRestTemplate.postForEntity(
                    eq(RESEND_API_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>("{\"id\":\"123\"}", HttpStatus.OK));

            emailService.sendEmail(TEST_RECIPIENT, TEST_SUBJECT, TEST_BODY, replyTo);

            ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(mockRestTemplate).postForEntity(eq(RESEND_API_URL), captor.capture(), eq(String.class));

            Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("reply_to")).isEqualTo(replyTo);
        }

        @Test
        @DisplayName("should not include reply-to when null")
        void shouldNotIncludeReplyToWhenNull() {
            when(mockRestTemplate.postForEntity(
                    eq(RESEND_API_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>("{\"id\":\"123\"}", HttpStatus.OK));

            emailService.sendEmail(TEST_RECIPIENT, TEST_SUBJECT, TEST_BODY, null);

            ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(mockRestTemplate).postForEntity(eq(RESEND_API_URL), captor.capture(), eq(String.class));

            Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
            assertThat(body).isNotNull();
            assertThat(body).doesNotContainKey("reply_to");
        }

        @Test
        @DisplayName("should not include reply-to when blank")
        void shouldNotIncludeReplyToWhenBlank() {
            when(mockRestTemplate.postForEntity(
                    eq(RESEND_API_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>("{\"id\":\"123\"}", HttpStatus.OK));

            emailService.sendEmail(TEST_RECIPIENT, TEST_SUBJECT, TEST_BODY, "   ");

            ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(mockRestTemplate).postForEntity(eq(RESEND_API_URL), captor.capture(), eq(String.class));

            Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
            assertThat(body).isNotNull();
            assertThat(body).doesNotContainKey("reply_to");
        }

        @Test
        @DisplayName("should skip sending when API key is not configured")
        void shouldSkipWhenNoApiKey() {
            ResendEmailService serviceWithoutKey = new ResendEmailService();
            RestTemplate mockTemplate = mock(RestTemplate.class);
            ReflectionTestUtils.setField(serviceWithoutKey, "apiKey", "");
            ReflectionTestUtils.setField(serviceWithoutKey, "fromEmail", TEST_FROM_EMAIL);
            ReflectionTestUtils.setField(serviceWithoutKey, "restTemplate", mockTemplate);

            serviceWithoutKey.sendEmail(TEST_RECIPIENT, TEST_SUBJECT, TEST_BODY, null);

            verify(mockTemplate, never()).postForEntity(anyString(), any(), any());
        }

        @Test
        @DisplayName("should skip sending when API key is blank")
        void shouldSkipWhenApiKeyIsBlank() {
            ResendEmailService serviceWithBlankKey = new ResendEmailService();
            RestTemplate mockTemplate = mock(RestTemplate.class);
            ReflectionTestUtils.setField(serviceWithBlankKey, "apiKey", "   ");
            ReflectionTestUtils.setField(serviceWithBlankKey, "fromEmail", TEST_FROM_EMAIL);
            ReflectionTestUtils.setField(serviceWithBlankKey, "restTemplate", mockTemplate);

            serviceWithBlankKey.sendEmail(TEST_RECIPIENT, TEST_SUBJECT, TEST_BODY, null);

            verify(mockTemplate, never()).postForEntity(anyString(), any(), any());
        }
    }

    // ========== Error Handling Tests (5 tests) ==========

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should throw RuntimeException on invalid email address (400)")
        void shouldHandleInvalidEmail() {
            when(mockRestTemplate.postForEntity(
                    eq(RESEND_API_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid 'to' email address"
            ));

            assertThatThrownBy(() ->
                    emailService.sendEmail("invalid-email", TEST_SUBJECT, TEST_BODY, null)
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to send email");
        }

        @Test
        @DisplayName("should throw RuntimeException on authentication failure (401)")
        void shouldHandleAuthFailure() {
            when(mockRestTemplate.postForEntity(
                    eq(RESEND_API_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new HttpClientErrorException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid API key"
            ));

            assertThatThrownBy(() ->
                    emailService.sendEmail(TEST_RECIPIENT, TEST_SUBJECT, TEST_BODY, null)
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to send email");
        }

        @Test
        @DisplayName("should throw RuntimeException on rate limit exceeded (429)")
        void shouldHandleRateLimit() {
            when(mockRestTemplate.postForEntity(
                    eq(RESEND_API_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new HttpClientErrorException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Rate limit exceeded"
            ));

            assertThatThrownBy(() ->
                    emailService.sendEmail(TEST_RECIPIENT, TEST_SUBJECT, TEST_BODY, null)
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to send email");
        }

        @Test
        @DisplayName("should throw RuntimeException on network timeout")
        void shouldHandleNetworkTimeout() {
            when(mockRestTemplate.postForEntity(
                    eq(RESEND_API_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new ResourceAccessException(
                    "I/O error on POST request",
                    new SocketTimeoutException("Read timed out")
            ));

            assertThatThrownBy(() ->
                    emailService.sendEmail(TEST_RECIPIENT, TEST_SUBJECT, TEST_BODY, null)
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to send email");
        }

        @Test
        @DisplayName("should throw RuntimeException on server error (500)")
        void shouldHandleServerError() {
            when(mockRestTemplate.postForEntity(
                    eq(RESEND_API_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new HttpServerErrorException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal server error"
            ));

            assertThatThrownBy(() ->
                    emailService.sendEmail(TEST_RECIPIENT, TEST_SUBJECT, TEST_BODY, null)
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to send email");
        }
    }

    // ========== Validation Tests (3 tests) ==========

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("should throw RuntimeException with null recipient")
        void shouldThrowOnNullRecipient() {
            // Service will throw NullPointerException because List.of() doesn't accept null
            assertThatThrownBy(() ->
                    emailService.sendEmail(null, TEST_SUBJECT, TEST_BODY, null)
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to send email")
                    .hasCauseInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should send email with empty subject")
        void shouldSendEmptySubject() {
            when(mockRestTemplate.postForEntity(
                    eq(RESEND_API_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>("{\"id\":\"123\"}", HttpStatus.OK));

            emailService.sendEmail(TEST_RECIPIENT, "", TEST_BODY, null);

            ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(mockRestTemplate).postForEntity(eq(RESEND_API_URL), captor.capture(), eq(String.class));

            Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("subject")).isEqualTo("");
        }

        @Test
        @DisplayName("should send email with empty body")
        void shouldSendEmptyBody() {
            when(mockRestTemplate.postForEntity(
                    eq(RESEND_API_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>("{\"id\":\"123\"}", HttpStatus.OK));

            emailService.sendEmail(TEST_RECIPIENT, TEST_SUBJECT, "", null);

            ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(mockRestTemplate).postForEntity(eq(RESEND_API_URL), captor.capture(), eq(String.class));

            Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("text")).isEqualTo("");
        }
    }

    // ========== HTML Email Tests (4 tests) ==========

    @Nested
    @DisplayName("HTML Email Sending")
    class HtmlEmailTests {

        @Test
        @DisplayName("should send HTML email with proper content type")
        void shouldSendHtmlWithContentType() {
            String htmlContent = "<html><body><h1>Welcome!</h1><p>Test email</p></body></html>";

            when(mockRestTemplate.postForEntity(
                    eq(RESEND_API_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>("{\"id\":\"123\"}", HttpStatus.OK));

            emailService.sendHtmlEmail(TEST_RECIPIENT, TEST_SUBJECT, htmlContent, null);

            ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(mockRestTemplate).postForEntity(eq(RESEND_API_URL), captor.capture(), eq(String.class));

            Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("html")).isEqualTo(htmlContent);
            assertThat(body.get("from")).isEqualTo(TEST_FROM_EMAIL);
            assertThat(body.get("to")).isEqualTo(List.of(TEST_RECIPIENT));
        }

        @Test
        @DisplayName("should handle HTML email with special characters")
        void shouldHandleHtmlWithSpecialChars() {
            String htmlContent = "<html><body><p>Special: &lt;, &gt;, &amp;</p></body></html>";

            when(mockRestTemplate.postForEntity(
                    eq(RESEND_API_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>("{\"id\":\"123\"}", HttpStatus.OK));

            emailService.sendHtmlEmail(TEST_RECIPIENT, TEST_SUBJECT, htmlContent, "reply@example.com");

            ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(mockRestTemplate).postForEntity(eq(RESEND_API_URL), captor.capture(), eq(String.class));

            Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("html")).isEqualTo(htmlContent);
            assertThat(body.get("reply_to")).isEqualTo("reply@example.com");
        }

        @Test
        @DisplayName("should skip HTML email when API key is blank")
        void shouldSkipHtmlEmailWithoutApiKey() {
            ResendEmailService serviceWithoutKey = new ResendEmailService();
            RestTemplate mockTemplate = mock(RestTemplate.class);
            ReflectionTestUtils.setField(serviceWithoutKey, "apiKey", "   ");
            ReflectionTestUtils.setField(serviceWithoutKey, "fromEmail", TEST_FROM_EMAIL);
            ReflectionTestUtils.setField(serviceWithoutKey, "restTemplate", mockTemplate);

            serviceWithoutKey.sendHtmlEmail(TEST_RECIPIENT, TEST_SUBJECT, "<h1>Test</h1>", null);

            verify(mockTemplate, never()).postForEntity(anyString(), any(), any());
        }

        @Test
        @DisplayName("should throw RuntimeException on HTML email error")
        void shouldHandleHtmlEmailError() {
            when(mockRestTemplate.postForEntity(
                    eq(RESEND_API_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new HttpServerErrorException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Server error"
            ));

            assertThatThrownBy(() ->
                    emailService.sendHtmlEmail(TEST_RECIPIENT, TEST_SUBJECT, "<h1>Test</h1>", null)
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to send email");
        }
    }
}
