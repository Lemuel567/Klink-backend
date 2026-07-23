package com.example.demo.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A Financial Secretary's reconciliation of church collections over a period
 * (one service day, or a whole month): for each payment type, the amount
 * recorded by hand plus the amount taken automatically through the app, and the
 * combined totals. Group dues are never included (that money is kept separate).
 */
@Getter
@Builder
public class CollectionsSummaryResponse {

    private LocalDate from;
    private LocalDate to;
    private List<Line> lines;
    private BigDecimal manualTotal;
    private BigDecimal onlineTotal;
    private BigDecimal grandTotal;

    @Getter
    @Builder
    public static class Line {
        private String type;        // OFFERING, TITHE, WELFARE, SPECIAL_CONTRIBUTION
        private BigDecimal manual;   // recorded by hand (cash counted by the secretary)
        private BigDecimal online;   // paid through the app (Paystack)
        private BigDecimal total;    // manual + online
    }
}
