package com.example.demo.dto.response;

import com.example.demo.model.Sermon;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class SermonResponse {

    private UUID id;
    private String preacher;
    private String title;
    private String memoryVerse;
    private String scripture;
    private LocalDate sermonDate;
    private String audioUrl;
    private String notes;
    private UUID postedBy;
    private LocalDateTime createdAt;

    public static SermonResponse from(Sermon sermon) {
        return SermonResponse.builder()
                .id(sermon.getId())
                .preacher(sermon.getPreacher())
                .title(sermon.getTitle())
                .memoryVerse(sermon.getMemoryVerse())
                .scripture(sermon.getScripture())
                .sermonDate(sermon.getSermonDate())
                .audioUrl(sermon.getAudioUrl())
                .notes(sermon.getNotes())
                .postedBy(sermon.getPostedBy())
                .createdAt(sermon.getCreatedAt())
                .build();
    }
}
