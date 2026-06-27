package com.example.demo.service;

public interface SmsService {
    void sendVerificationCode(String phoneNumber, String name, String rawCode);
    void sendSecurityAlert(String phoneNumber, String name, String message);
}
