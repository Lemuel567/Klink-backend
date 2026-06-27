package com.example.demo.dto.response;

import com.example.demo.model.Devotional;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DevotionalResponse {

    private UUID id;
    private String title;
    private String content;
    private LocalDate devotionalDate;
    private UUID postedBy;
    private LocalDateTime createdAt;

    public static DevotionalResponse from(Devotional devotional) {
        return DevotionalResponse.builder()
                .id(devotional.getId())
                .title(devotional.getTitle())
                .content(devotional.getContent())
                .devotionalDate(devotional.getDevotionalDate())
                .postedBy(devotional.getPostedBy())
                .createdAt(devotional.getCreatedAt())
                .build();
    }
}
