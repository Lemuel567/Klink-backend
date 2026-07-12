package com.example.demo.service;

public interface SmsService {
    void sendVerificationCode(String phoneNumber, String name, String rawCode);
    void sendSecurityAlert(String phoneNumber, String name, String message);

    /** General notification SMS. Returns true if the message was accepted by the provider. */
    boolean sendMessage(String phoneNumber, String message);
}
