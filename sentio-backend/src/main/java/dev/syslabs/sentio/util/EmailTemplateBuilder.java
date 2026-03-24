package dev.syslabs.sentio.util;

/**
 * Utility class for building consistent, branded HTML email templates.
 * Design follows the Sentio app's monochromatic/neutral design system.
 */
public class EmailTemplateBuilder {

    private EmailTemplateBuilder() {
    }

    /**
     * Build a standardized email HTML template with neutral/monochromatic design.
     */
    public static String buildEmail(String title, String bodyContent, String buttonText,
            String buttonUrl, String footerNote, String safetyNote) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; line-height: 1.6; color: #1a1a1a; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f5f5f5;">
                    <div style="background: #1a1a1a; padding: 40px 32px; border-radius: 12px 12px 0 0; text-align: center;">
                        <img src="https://sentio.syslabs.dev/sentio-logo-white.png" alt="Sentio" style="height: 40px; width: auto; margin-bottom: 16px;" />
                        <h1 style="color: rgba(255,255,255,0.9); margin: 0; font-size: 20px; font-weight: 400;">%s</h1>
                    </div>
                    <div style="background: #ffffff; padding: 32px; border: 1px solid #e5e5e5; border-top: none;">
                        %s
                        <div style="text-align: center; margin: 32px 0;">
                            <a href="%s" style="display: inline-block; background: #1a1a1a; color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 16px;">%s</a>
                        </div>
                        <p style="font-size: 14px; color: #666666; margin-bottom: 12px; text-align: center;">%s</p>
                        <p style="font-size: 14px; color: #666666; margin-bottom: 24px; text-align: center;">%s</p>
                    </div>
                    <div style="background: #ffffff; padding: 24px 32px; border: 1px solid #e5e5e5; border-top: none; border-radius: 0 0 12px 12px;">
                        <p style="font-size: 12px; color: #999999; text-align: center; margin: 0 0 12px 0;">
                            Can't click the button? Copy and paste this link:
                        </p>
                        <p style="font-size: 12px; color: #999999; text-align: center; margin: 0;">
                            <a href="%s" style="color: #1a1a1a; word-break: break-all;">%s</a>
                        </p>
                    </div>
                    <div style="text-align: center; padding: 24px; color: #999999; font-size: 12px;">
                        <p style="margin: 0;">© 2025 Sentio Systems. All rights reserved.</p>
                    </div>
                </body>
                </html>
                """
                .formatted(
                        title,
                        bodyContent,
                        buttonUrl,
                        buttonText,
                        footerNote,
                        safetyNote,
                        buttonUrl,
                        buttonUrl);
    }

    /**
     * Build password reset email.
     */
    public static String buildPasswordResetEmail(String resetUrl) {
        String bodyContent = """
                <p style="font-size: 16px; color: #1a1a1a; margin-bottom: 16px;">Hi there,</p>
                <p style="font-size: 16px; color: #333333; margin-bottom: 20px;">We received a request to reset your password. Click the button below to create a new password:</p>
                """;

        return buildEmail(
                "Reset Your Password",
                bodyContent,
                "Reset Password",
                resetUrl,
                "This link will expire in 1 hour for security reasons.",
                "If you didn't request this, you can safely ignore this email.");
    }

    /**
     * Build email verification email.
     */
    public static String buildVerificationEmail(String verifyUrl) {
        String bodyContent = """
                <p style="font-size: 16px; color: #1a1a1a; margin-bottom: 16px;">Thanks for signing up!</p>
                <p style="font-size: 16px; color: #333333; margin-bottom: 20px;">Please verify your email address to get started with Sentio. Click the button below to confirm your email:</p>
                """;

        return buildEmail(
                "Verify Your Email",
                bodyContent,
                "Verify Email",
                verifyUrl,
                "This link will expire in 24 hours.",
                "If you didn't create an account, you can safely ignore this email.");
    }
}
