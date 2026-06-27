package com.example.demo.dto.response;

import com.example.demo.model.Event;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class EventResponse {

    private UUID id;
    private String title;
    private String description;
    private String location;
    private String category;
    private LocalDateTime eventDate;
    private boolean reminderSent;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EventResponse from(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .category(event.getCategory())
                .eventDate(event.getEventDate())
                .reminderSent(event.isReminderSent())
                .createdBy(event.getCreatedBy())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
