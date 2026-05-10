package com.propertize.platform.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Auth-service email sender.
 *
 * Sends transactional emails for authentication flows:
 * - Password reset links
 * - Tenant credential provisioning
 *
 * In development the gateway routes SMTP to Mailpit (localhost:8025 for web
 * UI).
 * In production set SMTP_HOST / SMTP_PORT / SMTP_USERNAME / SMTP_PASSWORD env
 * vars.
 *
 * @author Propertize Platform
 * @since May 2026
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail-from-email:noreply@propertize.com}")
    private String fromEmail;

    @Value("${spring.mail-from-name:Propertize Team}")
    private String fromName;

    @Value("${spring.mail.host:localhost}")
    private String mailHost;

    @Value("${spring.mail.port:1025}")
    private int mailPort;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Send a password-reset link email.
     *
     * @param toEmail   recipient's email address
     * @param resetLink full URL the user clicks to reset their password
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String subject = "Reset your Propertize password";
        String htmlBody = buildPasswordResetHtml(toEmail, resetLink);
        sendHtml(toEmail, subject, htmlBody);
    }

    /**
     * Send login credentials to a newly provisioned tenant.
     *
     * @param toEmail  tenant's email
     * @param username assigned username
     * @param password temporary plain-text password
     * @param loginUrl URL of the tenant login page
     */
    @Async
    public void sendTenantCredentialsEmail(String toEmail, String username,
            String password, String loginUrl) {
        String subject = "Your Propertize tenant account is ready";
        String htmlBody = buildTenantCredentialsHtml(username, password, loginUrl);
        sendHtml(toEmail, subject, htmlBody);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void sendHtml(String to, String subject, String htmlBody) {
        if (isNotConfigured()) {
            log.warn("📧 [MAIL NOT CONFIGURED] SMTP host={}:{} — email NOT sent to: {} subject: {}",
                    mailHost, mailPort, to, subject);
            log.warn("   ➡  Start Mailpit (docker compose up mailpit) or set SMTP_HOST/SMTP_PORT env vars.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("✅ Email sent to: {}  subject: {}", to, subject);
        } catch (MessagingException | MailException e) {
            log.error("❌ Failed to send email to: {}  subject: {}  reason: {}", to, subject, e.getMessage(), e);
        } catch (java.io.UnsupportedEncodingException e) {
            log.error("❌ Encoding error sending email to: {}", to, e);
        }
    }

    /**
     * Returns true when SMTP is still pointing at the unconfigured localhost:25
     * default.
     * That means no real mail server is wired up yet.
     */
    private boolean isNotConfigured() {
        return "localhost".equals(mailHost) && mailPort == 25;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTML templates (inline — no Thymeleaf dependency in auth-service)
    // ─────────────────────────────────────────────────────────────────────────

    private String buildPasswordResetHtml(String email, String resetLink) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><title>Reset your password</title></head>
                <body style="font-family:Arial,sans-serif;background:#f4f4f4;padding:40px 0;margin:0">
                  <div style="max-width:520px;margin:0 auto;background:#fff;border-radius:8px;
                              padding:40px;box-shadow:0 2px 8px rgba(0,0,0,.08)">
                    <h2 style="color:#1a1a2e;margin-top:0">Reset your password</h2>
                    <p style="color:#555">We received a request to reset the password for <strong>%s</strong>.</p>
                    <p style="color:#555">Click the button below to choose a new password.
                       This link expires in <strong>24 hours</strong>.</p>
                    <div style="text-align:center;margin:32px 0">
                      <a href="%s"
                         style="background:#4f46e5;color:#fff;padding:14px 28px;border-radius:6px;
                                text-decoration:none;font-weight:600;display:inline-block">
                        Reset Password
                      </a>
                    </div>
                    <p style="color:#888;font-size:13px">
                      If you didn't request this, you can safely ignore this email.
                    </p>
                    <hr style="border:none;border-top:1px solid #eee;margin:32px 0">
                    <p style="color:#aaa;font-size:12px;text-align:center">
                      © 2026 Propertize · All rights reserved
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(email, resetLink);
    }

    private String buildTenantCredentialsHtml(String username, String password, String loginUrl) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><title>Your tenant account</title></head>
                <body style="font-family:Arial,sans-serif;background:#f4f4f4;padding:40px 0;margin:0">
                  <div style="max-width:520px;margin:0 auto;background:#fff;border-radius:8px;
                              padding:40px;box-shadow:0 2px 8px rgba(0,0,0,.08)">
                    <h2 style="color:#1a1a2e;margin-top:0">Your Propertize account is ready</h2>
                    <p style="color:#555">Here are your temporary login credentials:</p>
                    <table style="border-collapse:collapse;width:100%%;margin:16px 0">
                      <tr>
                        <td style="padding:10px;background:#f8f8f8;font-weight:600;width:40%%">Username</td>
                        <td style="padding:10px;background:#f8f8f8;font-family:monospace">%s</td>
                      </tr>
                      <tr>
                        <td style="padding:10px;font-weight:600">Password</td>
                        <td style="padding:10px;font-family:monospace">%s</td>
                      </tr>
                    </table>
                    <p style="color:#e74c3c;font-size:13px">
                      ⚠️ Please change your password after your first login.
                    </p>
                    <div style="text-align:center;margin:32px 0">
                      <a href="%s"
                         style="background:#4f46e5;color:#fff;padding:14px 28px;border-radius:6px;
                                text-decoration:none;font-weight:600;display:inline-block">
                        Login to Propertize
                      </a>
                    </div>
                    <hr style="border:none;border-top:1px solid #eee;margin:32px 0">
                    <p style="color:#aaa;font-size:12px;text-align:center">
                      © 2026 Propertize · All rights reserved
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(username, password, loginUrl);
    }
}
