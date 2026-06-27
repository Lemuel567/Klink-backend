package com.example.demo.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class VerificationEmailEvent extends ApplicationEvent {

    private final String email;
    private final String name;
    private final String rawCode;
    private final boolean passwordReset;

    public VerificationEmailEvent(Object source, String email, String name, String rawCode, boolean passwordReset) {
        super(source);
        this.email = email;
        this.name = name;
        this.rawCode = rawCode;
        this.passwordReset = passwordReset;
    }
}
