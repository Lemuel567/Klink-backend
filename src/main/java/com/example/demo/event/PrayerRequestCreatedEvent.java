package com.example.demo.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Published when a member submits a new prayer request. Handled after commit
 * to notify the church's Pastor(s) and Elder(s) only.
 */
@Getter
public class PrayerRequestCreatedEvent extends ApplicationEvent {

    private final UUID churchId;
    private final String requesterName;
    private final String title;

    public PrayerRequestCreatedEvent(Object source, UUID churchId, String requesterName, String title) {
        super(source);
        this.churchId = churchId;
        this.requesterName = requesterName;
        this.title = title;
    }
}
