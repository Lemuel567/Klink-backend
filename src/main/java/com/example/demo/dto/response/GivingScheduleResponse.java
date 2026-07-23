package com.example.demo.dto.response;

import com.example.demo.model.GivingSchedule;
import com.example.demo.model.OnlinePaymentType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class GivingScheduleResponse {

    private UUID id;
    private OnlinePaymentType paymentType;
    private BigDecimal amount;
    private int dayOfMonth;
    private boolean active;
    private String lastRunMonth;
    private LocalDateTime createdAt;

    public static GivingScheduleResponse from(GivingSchedule s) {
        return GivingScheduleResponse.builder()
                .id(s.getId())
                .paymentType(s.getPaymentType())
                .amount(s.getAmount())
                .dayOfMonth(s.getDayOfMonth())
                .active(s.isActive())
                .lastRunMonth(s.getLastRunMonth())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
