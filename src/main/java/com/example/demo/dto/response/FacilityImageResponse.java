package com.example.demo.dto.response;

import com.example.demo.model.FacilityImage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class FacilityImageResponse {

    private UUID id;
    private UUID facilityId;
    private String imageUrl;
    private String caption;
    private Boolean isPrimary;
    private UUID uploadedBy;
    private LocalDateTime uploadedAt;
    private Integer sortOrder;

    public static FacilityImageResponse from(FacilityImage img) {
        return FacilityImageResponse.builder()
                .id(img.getId())
                .facilityId(img.getFacility().getId())
                .imageUrl(img.getImageUrl())
                .caption(img.getCaption())
                .isPrimary(img.isPrimary())
                .uploadedBy(img.getUploadedBy())
                .uploadedAt(img.getUploadedAt())
                .sortOrder(img.getSortOrder())
                .build();
    }
}