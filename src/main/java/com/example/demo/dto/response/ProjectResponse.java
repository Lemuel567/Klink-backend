package com.example.demo.dto.response;

import com.example.demo.model.ChurchProject;
import com.example.demo.model.ProjectStatus;
import com.example.demo.model.ProjectType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Getter
@Builder
public class ProjectResponse {

    private UUID id;
    private UUID churchId;
    private String title;
    private String description;
    private ProjectType projectType;
    private ProjectStatus status;
    private BigDecimal targetAmount;
    private BigDecimal amountRaised;
    private String currency;
    private BigDecimal fundingPercentage;
    private BigDecimal remainingAmount;
    private long contributorCount;
    private Long daysRemaining;
    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private LocalDate actualEndDate;
    private String location;
    private String contractor;
    private UUID facilityId;
    private UUID createdBy;
    private UUID approvedBy;
    private LocalDateTime approvedAt;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectResponse from(ChurchProject p, long contributorCount) {
        BigDecimal raised = p.getAmountRaised() != null ? p.getAmountRaised() : BigDecimal.ZERO;
        BigDecimal target = p.getTargetAmount();
        BigDecimal fundingPct = target.compareTo(BigDecimal.ZERO) > 0
                ? raised.divide(target, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal remaining = target.subtract(raised).max(BigDecimal.ZERO);
        Long daysRemaining = null;
        if (p.getExpectedEndDate() != null && p.getStatus() != ProjectStatus.COMPLETED && p.getStatus() != ProjectStatus.CANCELLED) {
            daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), p.getExpectedEndDate());
        }

        return ProjectResponse.builder()
                .id(p.getId())
                .churchId(p.getChurch().getId())
                .title(p.getTitle())
                .description(p.getDescription())
                .projectType(p.getProjectType())
                .status(p.getStatus())
                .targetAmount(target)
                .amountRaised(raised)
                .currency(p.getCurrency())
                .fundingPercentage(fundingPct)
                .remainingAmount(remaining)
                .contributorCount(contributorCount)
                .daysRemaining(daysRemaining)
                .startDate(p.getStartDate())
                .expectedEndDate(p.getExpectedEndDate())
                .actualEndDate(p.getActualEndDate())
                .location(p.getLocation())
                .contractor(p.getContractor())
                .facilityId(p.getFacilityId())
                .createdBy(p.getCreatedBy())
                .approvedBy(p.getApprovedBy())
                .approvedAt(p.getApprovedAt())
                .isPublic(p.isPublic())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}