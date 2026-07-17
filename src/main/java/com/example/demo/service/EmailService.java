package com.example.demo.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    // Brand palette (matches the app): deep purple + gold accent.
    private static final String PURPLE = "#2D1B69";
    private static final String GOLD = "#F4A429";

    public void sendVerificationCode(String toEmail, String memberName, String code) {
        String html = shell("Verify your email",
                "<p style=\"margin:0 0 16px;color:#3b3350;font-size:15px;line-height:1.6\">Hi "
                        + esc(firstName(memberName)) + ",</p>"
                        + "<p style=\"margin:0 0 20px;color:#3b3350;font-size:15px;line-height:1.6\">"
                        + "Use this code to verify your email address and finish setting up your Klink account.</p>"
                        + codeBox(code)
                        + "<p style=\"margin:20px 0 0;color:#6b6480;font-size:13px;line-height:1.6\">"
                        + "This code expires in 15 minutes. If you didn't create a Klink account, you can safely ignore this email.</p>");
        String text = "Hi " + firstName(memberName) + ",\n\n"
                + "Your Klink email verification code is: " + code + "\n\n"
                + "This code expires in 15 minutes.\n\n"
                + "If you did not create a Klink account, please ignore this email.\n\nGod bless,\nThe Klink Team";
        send(toEmail, "Klink — Verify Your Email", html, text);
    }

    public void sendPasswordResetCode(String toEmail, String memberName, String code) {
        String html = shell("Reset your password",
                "<p style=\"margin:0 0 16px;color:#3b3350;font-size:15px;line-height:1.6\">Hi "
                        + esc(firstName(memberName)) + ",</p>"
                        + "<p style=\"margin:0 0 20px;color:#3b3350;font-size:15px;line-height:1.6\">"
                        + "You asked to reset your password. Enter this code in the app to choose a new one.</p>"
                        + codeBox(code)
                        + "<p style=\"margin:20px 0 0;color:#6b6480;font-size:13px;line-height:1.6\">"
                        + "This code expires in 15 minutes. If you didn't request a reset, ignore this email — your password stays the same.</p>");
        String text = "Hi " + firstName(memberName) + ",\n\n"
                + "Your Klink password reset code is: " + code + "\n\n"
                + "This code expires in 15 minutes.\n\n"
                + "If you did not request this, please ignore this email.\n\nGod bless,\nThe Klink Team";
        send(toEmail, "Klink — Password Reset Code", html, text);
    }

    public void sendPaymentReceipt(String toEmail, String memberName, String churchName,
                                   String amountGhs, String paymentTypeLabel,
                                   String reference, String channel) {
        String channelLabel = channel == null ? "online" : channel.replace('_', ' ');
        String html = shell("Payment receipt",
                "<p style=\"margin:0 0 16px;color:#3b3350;font-size:15px;line-height:1.6\">Dear "
                        + esc(firstName(memberName)) + ",</p>"
                        + "<p style=\"margin:0 0 20px;color:#3b3350;font-size:15px;line-height:1.6\">"
                        + "Thank you for your faithful giving to " + esc(churchName) + ".</p>"
                        + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                        + "style=\"border:1px solid #ece7f5;border-radius:12px;overflow:hidden;margin:0 0 20px\">"
                        + receiptRow("Amount", "GHS " + esc(amountGhs), true)
                        + receiptRow("Type", esc(paymentTypeLabel), false)
                        + receiptRow("Reference", esc(reference), false)
                        + receiptRow("Channel", esc(channelLabel), false)
                        + "</table>"
                        + "<p style=\"margin:0;color:#6b6480;font-size:13px;font-style:italic;line-height:1.6\">"
                        + "&ldquo;Each of you should give what you have decided in your heart to give.&rdquo; — 2 Corinthians 9:7</p>");
        String text = "Dear " + firstName(memberName) + ",\n\n"
                + "Thank you for your faithful giving to " + churchName + ".\n\n"
                + "Payment Details:\n"
                + "Amount: GHS " + amountGhs + "\n"
                + "Type: " + paymentTypeLabel + "\n"
                + "Reference: " + reference + "\n"
                + "Channel: " + channelLabel + "\n\n"
                + "\"Each of you should give what you have decided in your heart to give.\" — 2 Corinthians 9:7\n\n"
                + "God bless you for your faithfulness.\n\n" + churchName + "\nPowered by Klink";
        send(toEmail, "Payment Receipt — Klink Church App", html, text);
    }

    // ── HTML building blocks ────────────────────────────────────────────────

    /** Outer branded shell shared by every email (header, card, footer). */
    private String shell(String heading, String bodyHtml) {
        return "<!doctype html><html><body style=\"margin:0;padding:0;background:#f4f1fb;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f4f1fb;padding:24px 0;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"max-width:480px;background:#ffffff;border-radius:16px;overflow:hidden;"
                + "font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;box-shadow:0 6px 24px rgba(45,27,105,0.08);\">"
                // Header
                + "<tr><td style=\"background:" + PURPLE + ";padding:28px 32px;text-align:center;\">"
                + "<span style=\"display:inline-block;width:44px;height:44px;line-height:44px;border-radius:50%;"
                + "background:" + GOLD + ";color:" + PURPLE + ";font-size:24px;font-weight:700;\">K</span>"
                + "<div style=\"color:#ffffff;font-size:20px;font-weight:700;letter-spacing:0.5px;margin-top:10px;\">Klink</div>"
                + "</td></tr>"
                // Body
                + "<tr><td style=\"padding:32px;\">"
                + "<h1 style=\"margin:0 0 20px;color:" + PURPLE + ";font-size:20px;\">" + esc(heading) + "</h1>"
                + bodyHtml
                + "</td></tr>"
                // Footer
                + "<tr><td style=\"padding:20px 32px;border-top:1px solid #ece7f5;text-align:center;\">"
                + "<div style=\"color:#9a93ad;font-size:12px;line-height:1.6;\">God bless,<br/>The Klink Team</div>"
                + "</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    private String codeBox(String code) {
        return "<div style=\"text-align:center;background:#faf6ff;border:1px solid " + GOLD + ";"
                + "border-radius:12px;padding:18px;margin:8px 0;\">"
                + "<div style=\"color:" + GOLD + ";font-size:12px;font-weight:600;letter-spacing:2px;\">YOUR CODE</div>"
                + "<div style=\"color:" + PURPLE + ";font-size:34px;font-weight:700;letter-spacing:8px;margin-top:6px;\">"
                + esc(code) + "</div></div>";
    }

    private String receiptRow(String label, String value, boolean highlight) {
        String valueColor = highlight ? GOLD : "#2b2540";
        return "<tr><td style=\"padding:12px 16px;border-bottom:1px solid #ece7f5;color:#6b6480;font-size:14px;\">" + label + "</td>"
                + "<td style=\"padding:12px 16px;border-bottom:1px solid #ece7f5;text-align:right;color:" + valueColor
                + ";font-size:14px;font-weight:600;\">" + value + "</td></tr>";
    }

    // ── Send + helpers ──────────────────────────────────────────────────────

    /** Sends a multipart email (HTML with a plain-text fallback). Degrades silently if mail is unconfigured. */
    private void send(String to, String subject, String html, String text) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("Mail not configured. Skipping email to {}", to);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html); // (plain, html) → multipart/alternative
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String firstName(String name) {
        if (name == null || name.isBlank()) return "there";
        return name.trim().split("\\s+")[0];
    }

    /** Minimal HTML escaping for user-supplied values interpolated into the template. */
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
