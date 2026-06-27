package com.example.demo.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class NotificationEvent extends ApplicationEvent {

    private final UUID churchId;
    private final String title;
    private final String body;

    public NotificationEvent(Object source, UUID churchId, String title, String body) {
        super(source);
        this.churchId = churchId;
        this.title = title;
        this.body = body;
    }
}
