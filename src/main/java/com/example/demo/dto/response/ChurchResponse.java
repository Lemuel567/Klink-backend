package com.example.demo.dto.response;

import com.example.demo.model.Church;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ChurchResponse {

    private UUID id;
    private String churchName;
    private String location;
    private String denomination;
    private String churchCode;
    private BigDecimal welfareAmount;
    private String contactPhone;
    private String contactEmail;
    private String photoUrl;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private LocalDateTime scheduledDeletionAt;

    public static ChurchResponse from(Church church) {
        return ChurchResponse.builder()
                .id(church.getId())
                .churchName(church.getChurchName())
                .location(church.getLocation())
                .denomination(church.getDenomination())
                .churchCode(church.getChurchCode())
                .welfareAmount(church.getWelfareAmount())
                .contactPhone(church.getContactPhone())
                .contactEmail(church.getContactEmail())
                .photoUrl(church.getPhotoUrl())
                .createdAt(church.getCreatedAt())
                .deletedAt(church.getDeletedAt())
                .scheduledDeletionAt(church.getDeletedAt() != null
                        ? church.getDeletedAt().plusDays(30) : null)
                .build();
    }
}
