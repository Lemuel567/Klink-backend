package com.example.demo.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SmsVerificationEvent extends ApplicationEvent {

    private final String phoneNumber;
    private final String name;
    private final String rawCode;

    public SmsVerificationEvent(Object source, String phoneNumber, String name, String rawCode) {
        super(source);
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.rawCode = rawCode;
    }
}
