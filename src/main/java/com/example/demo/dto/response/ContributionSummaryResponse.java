package com.example.demo.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class ContributionSummaryResponse {

    private BigDecimal targetAmount;
    private BigDecimal amountRaised;
    private BigDecimal fundingPercentage;
    private BigDecimal remainingAmount;
    private long contributorCount;
    private BigDecimal averageContribution;
    private BigDecimal largestContribution;
    private LocalDate mostRecentContributionDate;
}