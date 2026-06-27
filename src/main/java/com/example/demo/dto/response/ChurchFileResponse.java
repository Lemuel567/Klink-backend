package com.example.demo.dto.response;

import com.example.demo.model.ChurchFile;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ChurchFileResponse {

    private UUID id;
    private String title;
    private String category;
    private String language;
    private String fileUrl;
    private UUID uploadedBy;
    private LocalDateTime uploadedAt;

    public static ChurchFileResponse from(ChurchFile file) {
        return ChurchFileResponse.builder()
                .id(file.getId())
                .title(file.getTitle())
                .category(file.getCategory())
                .language(file.getLanguage())
                .fileUrl(file.getFileUrl())
                .uploadedBy(file.getUploadedBy())
                .uploadedAt(file.getUploadedAt())
                .build();
    }
}
