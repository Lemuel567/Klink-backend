package com.example.demo.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;
import java.util.UUID;

/**
 * Notification for a specific set of members (not the whole church).
 * Published inside a transaction and handled AFTER_COMMIT so notifications
 * are never sent for work that rolled back, and never block the request thread.
 */
@Getter
public class TargetedNotificationEvent extends ApplicationEvent {

    private final UUID churchId;
    private final List<UUID> memberIds;
    private final String title;
    private final String body;

    public TargetedNotificationEvent(Object source, UUID churchId, List<UUID> memberIds, String title, String body) {
        super(source);
        this.churchId = churchId;
        this.memberIds = List.copyOf(memberIds);
        this.title = title;
        this.body = body;
    }
}
