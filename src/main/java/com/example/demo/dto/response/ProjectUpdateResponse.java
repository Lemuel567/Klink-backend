package com.example.demo.dto.response;

import com.example.demo.model.ProjectUpdate;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ProjectUpdateResponse {

    private UUID id;
    private UUID projectId;
    private String title;
    private String content;
    private UUID postedBy;
    private LocalDateTime postedAt;
    private LocalDateTime updatedAt;

    public static ProjectUpdateResponse from(ProjectUpdate u) {
        return ProjectUpdateResponse.builder()
                .id(u.getId())
                .projectId(u.getProject().getId())
                .title(u.getTitle())
                .content(u.getContent())
                .postedBy(u.getPostedBy())
                .postedAt(u.getPostedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}