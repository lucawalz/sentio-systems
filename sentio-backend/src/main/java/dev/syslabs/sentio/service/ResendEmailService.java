package dev.syslabs.sentio.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Email service using Resend API for transactional emails.
 * 
 * @see <a href="https://resend.com/docs/api-reference/emails/send-email">Resend
 *      API Docs</a>
 */
@Service
@Slf4j
public class ResendEmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final RestTemplate restTemplate;

    @Value("${resend.api-key:}")
    private String apiKey;

    @Value("${resend.from-email:noreply@syslabs.dev}")
    private String fromEmail;

    public ResendEmailService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Send an email using Resend API.
     *
     * @param to      Recipient email address
     * @param subject Email subject
     * @param text    Plain text content
     * @param replyTo Optional reply-to address
     */
    public void sendEmail(String to, String subject, String text, String replyTo) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Resend API key not configured, skipping email send");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new java.util.HashMap<>();
            body.put("from", fromEmail);
            body.put("to", List.of(to));
            body.put("subject", subject);
            body.put("text", text);

            if (replyTo != null && !replyTo.isBlank()) {
                body.put("reply_to", replyTo);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(RESEND_API_URL, request, String.class);
            log.info("Email sent successfully to {}", to);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send an HTML email using Resend API.
     */
    public void sendHtmlEmail(String to, String subject, String html, String replyTo) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Resend API key not configured, skipping email send");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new java.util.HashMap<>();
            body.put("from", fromEmail);
            body.put("to", List.of(to));
            body.put("subject", subject);
            body.put("html", html);

            if (replyTo != null && !replyTo.isBlank()) {
                body.put("reply_to", replyTo);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(RESEND_API_URL, request, String.class);
            log.info("HTML email sent successfully to {}", to);

        } catch (Exception e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
