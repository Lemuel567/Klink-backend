package com.example.demo.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Group finances — kept completely separate from church finances. Every amount
 * here is drawn only from payments whose group_id is this group; church
 * offering/tithe/welfare are never included, and these dues never appear in any
 * church total.
 */
@Getter
@Builder
public class GroupFinanceSummaryResponse {

    private BigDecimal totalCollected;      // all-time CONFIRMED dues for this group
    private BigDecimal thisMonthCollected;  // CONFIRMED dues for the current month
    private String currentMonth;            // YYYY-MM
    private long memberCount;
    private long paidThisMonth;             // members who have paid this month
    private BigDecimal duesAmount;          // the group's monthly dues figure
    private List<PaymentResponse> recentPayments;

    public static GroupFinanceSummaryResponse of(
            BigDecimal totalCollected,
            BigDecimal thisMonthCollected,
            String currentMonth,
            long memberCount,
            long paidThisMonth,
            BigDecimal duesAmount,
            List<PaymentResponse> recentPayments) {
        return GroupFinanceSummaryResponse.builder()
                .totalCollected(totalCollected)
                .thisMonthCollected(thisMonthCollected)
                .currentMonth(currentMonth)
                .memberCount(memberCount)
                .paidThisMonth(paidThisMonth)
                .duesAmount(duesAmount)
                .recentPayments(recentPayments)
                .build();
    }
}
