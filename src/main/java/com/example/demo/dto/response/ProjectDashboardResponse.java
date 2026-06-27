package com.example.demo.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ProjectDashboardResponse {

    private Map<String, Long> totalProjectsByStatus;
    private BigDecimal totalTargetAmount;
    private BigDecimal totalAmountRaised;
    private BigDecimal overallFundingPercentage;
    private List<ProjectResponse> projectsNeedingAttention;
    private List<ProjectUpdateResponse> recentUpdates;
    private List<TopContributorEntry> topContributors;

    @Getter
    @Builder
    public static class TopContributorEntry {
        private String memberName;
        private BigDecimal totalAmount;
    }
}