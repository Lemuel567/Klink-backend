package com.example.demo.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class PaymentSummaryResponse {

    private BigDecimal totalThisMonth;
    private BigDecimal totalThisYear;
    private long successCount;
    private long pendingCount;
    private long failedCount;
    private Map<String, Long> countByPaymentType;
    private long mobileMoneyCount;
    private long cardCount;
    private List<OnlinePaymentResponse> recentPayments;
}
