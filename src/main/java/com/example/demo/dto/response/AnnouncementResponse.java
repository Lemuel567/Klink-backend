package com.example.demo.dto.response;

import com.example.demo.model.Announcement;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AnnouncementResponse {

    private UUID id;
    private String title;
    private String body;
    private String flyerUrl;
    private UUID postedBy;
    private LocalDateTime createdAt;

    public static AnnouncementResponse from(Announcement announcement) {
        return AnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .body(announcement.getBody())
                .flyerUrl(announcement.getFlyerUrl())
                .postedBy(announcement.getPostedBy())
                .createdAt(announcement.getCreatedAt())
                .build();
    }
}
