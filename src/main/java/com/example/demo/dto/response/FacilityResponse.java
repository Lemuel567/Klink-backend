package com.example.demo.dto.response;

import com.example.demo.model.Facility;
import com.example.demo.model.FacilityCondition;
import com.example.demo.model.FacilityType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class FacilityResponse {

    private UUID id;
    private UUID churchId;
    private String name;
    private String description;
    private FacilityType facilityType;
    private String address;
    private Integer capacity;
    private Integer yearAcquired;
    private BigDecimal estimatedValue;
    private String currency;
    private FacilityCondition condition;
    private Boolean isActive;
    private String notes;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FacilityResponse from(Facility f) {
        return FacilityResponse.builder()
                .id(f.getId())
                .churchId(f.getChurch().getId())
                .name(f.getName())
                .description(f.getDescription())
                .facilityType(f.getFacilityType())
                .address(f.getAddress())
                .capacity(f.getCapacity())
                .yearAcquired(f.getYearAcquired())
                .estimatedValue(f.getEstimatedValue())
                .currency(f.getCurrency())
                .condition(f.getCondition())
                .isActive(f.isActive())
                .notes(f.getNotes())
                .createdBy(f.getCreatedBy())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }
}