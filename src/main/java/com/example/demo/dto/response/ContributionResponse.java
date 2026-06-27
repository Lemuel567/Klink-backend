package com.example.demo.dto.response;

import com.example.demo.model.ContributionPaymentMethod;
import com.example.demo.model.ProjectContribution;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ContributionResponse {

    private UUID id;
    private UUID projectId;
    private UUID memberId;
    private String memberName;
    private BigDecimal amount;
    private String currency;
    private LocalDate contributionDate;
    private ContributionPaymentMethod paymentMethod;
    private UUID recordedBy;
    private String notes;
    private LocalDateTime createdAt;

    public static ContributionResponse from(ProjectContribution c) {
        return ContributionResponse.builder()
                .id(c.getId())
                .projectId(c.getProject().getId())
                .memberId(c.getMember().getId())
                .memberName(c.getMember().getFullName())
                .amount(c.getAmount())
                .currency(c.getCurrency())
                .contributionDate(c.getContributionDate())
                .paymentMethod(c.getPaymentMethod())
                .recordedBy(c.getRecordedBy())
                .notes(c.getNotes())
                .createdAt(c.getCreatedAt())
                .build();
    }

    public static ContributionResponse fromAnonymous(ProjectContribution c) {
        return ContributionResponse.builder()
                .id(c.getId())
                .projectId(c.getProject().getId())
                .memberId(null)
                .memberName(null)
                .amount(c.getAmount())
                .currency(c.getCurrency())
                .contributionDate(c.getContributionDate())
                .paymentMethod(c.getPaymentMethod())
                .recordedBy(null)
                .notes(null)
                .createdAt(c.getCreatedAt())
                .build();
    }
}