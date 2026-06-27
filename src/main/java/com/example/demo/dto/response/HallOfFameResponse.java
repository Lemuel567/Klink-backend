package com.example.demo.dto.response;

import com.example.demo.model.HallOfFame;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class HallOfFameResponse {

    private UUID id;
    private UUID memberId;
    private String memberName;
    private String title;
    private String description;
    private String photoUrl;
    private UUID postedBy;
    private LocalDateTime createdAt;

    public static HallOfFameResponse from(HallOfFame entry) {
        return HallOfFameResponse.builder()
                .id(entry.getId())
                .memberId(entry.getMember() != null ? entry.getMember().getId() : null)
                .memberName(entry.getMember() != null ? entry.getMember().getFullName() : null)
                .title(entry.getTitle())
                .description(entry.getDescription())
                .photoUrl(entry.getPhotoUrl())
                .postedBy(entry.getPostedBy())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
