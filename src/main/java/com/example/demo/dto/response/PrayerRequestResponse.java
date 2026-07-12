package com.example.demo.dto.response;

import com.example.demo.model.PrayerRequest;
import com.example.demo.model.PrayerStatus;
import com.example.demo.model.PrayerVisibility;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class PrayerRequestResponse {

    private UUID id;
    private UUID memberId;
    private String memberName;
    private String title;
    private String content;
    private PrayerVisibility visibility;
    private PrayerStatus status;
    private String leaderResponse;
    private UUID answeredBy;
    private LocalDateTime answeredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PrayerRequestResponse from(PrayerRequest p, String memberName) {
        return PrayerRequestResponse.builder()
                .id(p.getId())
                .memberId(p.getMemberId())
                .memberName(memberName)
                .title(p.getTitle())
                .content(p.getContent())
                .visibility(p.getVisibility())
                .status(p.getStatus())
                .leaderResponse(p.getLeaderResponse())
                .answeredBy(p.getAnsweredBy())
                .answeredAt(p.getAnsweredAt())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
