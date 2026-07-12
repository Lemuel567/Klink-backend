package com.example.demo.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TwilioSmsService implements SmsService {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.from-number:}")
    private String fromNumber;

    @PostConstruct
    void init() {
        if (!accountSid.isBlank() && !authToken.isBlank()) {
            Twilio.init(accountSid, authToken);
            log.info("[SMS] Twilio initialized from-number={}", fromNumber);
        } else {
            log.warn("[SMS] Twilio credentials not configured — SMS sending is disabled");
        }
    }

    @Override
    public void sendVerificationCode(String phoneNumber, String name, String rawCode) {
        if (accountSid.isBlank()) {
            log.warn("[SMS] PHONE_VERIFICATION_SKIPPED (Twilio not configured) to=...{}", last4(phoneNumber));
            return;
        }
        String body = "Your Klink verification code is " + rawCode + ". It expires in 10 minutes. Do not share this code.";
        try {
            Message.creator(new PhoneNumber(phoneNumber), new PhoneNumber(fromNumber), body).create();
            log.info("[SMS] PHONE_VERIFICATION_SENT to=...{}", last4(phoneNumber));
        } catch (Exception e) {
            log.error("[SMS] Send failed to=...{} reason={}", last4(phoneNumber), e.getMessage());
            throw e;
        }
    }

    @Override
    public void sendSecurityAlert(String phoneNumber, String name, String message) {
        if (accountSid.isBlank()) return;
        try {
            Message.creator(new PhoneNumber(phoneNumber), new PhoneNumber(fromNumber), message).create();
            log.info("[SMS] SECURITY_ALERT sent to=...{}", last4(phoneNumber));
        } catch (Exception e) {
            log.error("[SMS] Security alert failed to=...{} reason={}", last4(phoneNumber), e.getMessage());
        }
    }

    @Override
    public boolean sendMessage(String phoneNumber, String message) {
        if (accountSid.isBlank() || phoneNumber == null || phoneNumber.isBlank()) return false;
        try {
            Message.creator(new PhoneNumber(phoneNumber), new PhoneNumber(fromNumber), message).create();
            log.info("[SMS] NOTIFICATION sent to=...{}", last4(phoneNumber));
            return true;
        } catch (Exception e) {
            log.error("[SMS] Notification failed to=...{} reason={}", last4(phoneNumber), e.getMessage());
            return false;
        }
    }

    private String last4(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) return "????";
        return phoneNumber.substring(phoneNumber.length() - 4);
    }
}
