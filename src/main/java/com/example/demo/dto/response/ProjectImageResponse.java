package com.example.demo.dto.response;

import com.example.demo.model.ProjectImage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ProjectImageResponse {

    private UUID id;
    private UUID projectId;
    private UUID updateId;
    private String imageUrl;
    private String caption;
    private Boolean isPrimary;
    private String phase;
    private UUID uploadedBy;
    private LocalDateTime uploadedAt;
    private Integer sortOrder;

    public static ProjectImageResponse from(ProjectImage img) {
        return ProjectImageResponse.builder()
                .id(img.getId())
                .projectId(img.getProject().getId())
                .updateId(img.getUpdateId())
                .imageUrl(img.getImageUrl())
                .caption(img.getCaption())
                .isPrimary(img.isPrimary())
                .phase(img.getPhase())
                .uploadedBy(img.getUploadedBy())
                .uploadedAt(img.getUploadedAt())
                .sortOrder(img.getSortOrder())
                .build();
    }
}