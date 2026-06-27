package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void sendVerificationCode(String toEmail, String memberName, String code) {
        send(toEmail,
                "Klink — Verify Your Email",
                "Hi " + memberName + ",\n\n" +
                "Your email verification code is: " + code + "\n\n" +
                "This code expires in 15 minutes.\n\n" +
                "If you did not create a Klink account, please ignore this email.\n\n" +
                "God bless,\nThe Klink Team");
    }

    public void sendPasswordResetCode(String toEmail, String memberName, String code) {
        send(toEmail,
                "Klink — Password Reset Code",
                "Hi " + memberName + ",\n\n" +
                "You requested a password reset. Your code is: " + code + "\n\n" +
                "This code expires in 15 minutes.\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "God bless,\nThe Klink Team");
    }

    private void send(String to, String subject, String text) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("Mail not configured. Skipping email to {}", to);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
