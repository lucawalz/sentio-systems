package dev.syslabs.sentio.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmailTemplateBuilder")
class EmailTemplateBuilderTest {

    @Test
    @DisplayName("buildPasswordResetEmail should contain reset URL")
    void buildPasswordResetEmail_containsUrl() {
        // Arrange
        String resetUrl = "https://sentio.syslabs.dev/reset-password?token=abc123";

        // Act
        String email = EmailTemplateBuilder.buildPasswordResetEmail(resetUrl);

        // Assert
        assertThat(email).contains(resetUrl);
        assertThat(email).contains("Reset Your Password");
        assertThat(email).contains("Reset Password");
    }

    @Test
    @DisplayName("buildPasswordResetEmail should contain button and fallback link")
    void buildPasswordResetEmail_containsButtonAndFallback() {
        // Arrange
        String resetUrl = "https://sentio.syslabs.dev/reset-password?token=test";

        // Act
        String email = EmailTemplateBuilder.buildPasswordResetEmail(resetUrl);

        // Assert
        // Button link
        assertThat(email).contains("<a href=\"" + resetUrl + "\"");
        // Fallback text
        assertThat(email).contains("Can't click the button?");
        // Multiple occurrences of URL (button + fallback) - count manually
        int count = 0;
        int index = 0;
        while ((index = email.indexOf(resetUrl, index)) != -1) {
            count++;
            index += resetUrl.length();
        }
        assertThat(count).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("buildPasswordResetEmail should contain security notes")
    void buildPasswordResetEmail_containsSecurityNotes() {
        // Arrange
        String resetUrl = "https://sentio.syslabs.dev/reset-password?token=test";

        // Act
        String email = EmailTemplateBuilder.buildPasswordResetEmail(resetUrl);

        // Assert
        assertThat(email).contains("This link will expire in 1 hour");
        assertThat(email).contains("If you didn't request this");
    }

    @Test
    @DisplayName("buildVerificationEmail should contain verification URL")
    void buildVerificationEmail_containsUrl() {
        // Arrange
        String verifyUrl = "https://sentio.syslabs.dev/verify-email?token=xyz789";

        // Act
        String email = EmailTemplateBuilder.buildVerificationEmail(verifyUrl);

        // Assert
        assertThat(email).contains(verifyUrl);
        assertThat(email).contains("Verify Your Email");
        assertThat(email).contains("Verify Email");
    }

    @Test
    @DisplayName("buildVerificationEmail should contain button and fallback link")
    void buildVerificationEmail_containsButtonAndFallback() {
        // Arrange
        String verifyUrl = "https://sentio.syslabs.dev/verify-email?token=test";

        // Act
        String email = EmailTemplateBuilder.buildVerificationEmail(verifyUrl);

        // Assert
        // Button link
        assertThat(email).contains("<a href=\"" + verifyUrl + "\"");
        // Fallback text
        assertThat(email).contains("Can't click the button?");
        // Multiple occurrences of URL - count manually
        int count = 0;
        int index = 0;
        while ((index = email.indexOf(verifyUrl, index)) != -1) {
            count++;
            index += verifyUrl.length();
        }
        assertThat(count).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("buildVerificationEmail should contain appropriate timing and safety notes")
    void buildVerificationEmail_containsNotes() {
        // Arrange
        String verifyUrl = "https://sentio.syslabs.dev/verify-email?token=test";

        // Act
        String email = EmailTemplateBuilder.buildVerificationEmail(verifyUrl);

        // Assert
        assertThat(email).contains("This link will expire in 24 hours");
        assertThat(email).contains("If you didn't create an account");
    }

    @Test
    @DisplayName("buildEmail should contain Sentio branding")
    void buildEmail_containsBranding() {
        // Arrange
        String url = "https://sentio.syslabs.dev/test";

        // Act
        String email = EmailTemplateBuilder.buildEmail(
                "Test Title",
                "<p>Test body content</p>",
                "Click Here",
                url,
                "Test footer",
                "Test safety note"
        );

        // Assert
        assertThat(email).contains("Sentio");
        assertThat(email).contains("sentio-logo-white.png");
        assertThat(email).contains("© 2025 Sentio Systems");
    }

    @Test
    @DisplayName("buildEmail should properly format all parameters")
    void buildEmail_formatsAllParameters() {
        // Arrange
        String title = "Custom Title";
        String bodyContent = "<p>Custom body content with <strong>formatting</strong></p>";
        String buttonText = "Custom Button";
        String url = "https://example.com/custom-url";
        String footerNote = "Custom footer note";
        String safetyNote = "Custom safety note";

        // Act
        String email = EmailTemplateBuilder.buildEmail(
                title,
                bodyContent,
                buttonText,
                url,
                footerNote,
                safetyNote
        );

        // Assert
        assertThat(email).contains(title);
        assertThat(email).contains(bodyContent);
        assertThat(email).contains(buttonText);
        assertThat(email).contains(url);
        assertThat(email).contains(footerNote);
        assertThat(email).contains(safetyNote);
    }

    @Test
    @DisplayName("buildEmail should be valid HTML")
    void buildEmail_isValidHtml() {
        // Arrange
        String url = "https://sentio.syslabs.dev/test";

        // Act
        String email = EmailTemplateBuilder.buildEmail(
                "Test",
                "<p>Test</p>",
                "Click",
                url,
                "Footer",
                "Safety"
        );

        // Assert
        assertThat(email).startsWith("<!DOCTYPE html>");
        assertThat(email).contains("<html>");
        assertThat(email).contains("</html>");
        assertThat(email).contains("<head>");
        assertThat(email).contains("</head>");
        assertThat(email).contains("<body");
        assertThat(email).contains("</body>");
    }
}
