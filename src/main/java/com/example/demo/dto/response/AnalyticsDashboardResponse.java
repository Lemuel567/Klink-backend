package com.example.demo.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Leadership dashboard: month-by-month trends (oldest → newest) for attendance,
 * giving and new members, plus headline totals and month-over-month growth.
 */
@Getter
@Builder
public class AnalyticsDashboardResponse {

    private List<String> months;          // e.g. ["2026-02", "2026-03", ...]
    private List<Long> attendance;        // present count per month
    private List<BigDecimal> giving;      // confirmed church giving per month
    private List<Long> newMembers;        // members that joined per month

    private long totalMembers;
    private long activeMembers;
    private long thisMonthAttendance;
    private BigDecimal thisMonthGiving;

    private double attendanceGrowthPct;   // this month vs last month
    private double givingGrowthPct;
    private long newMembersThisMonth;
}
