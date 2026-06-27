package com.example.demo.dto.response;

import com.example.demo.model.Pledge;
import com.example.demo.model.PledgeStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class PledgeResponse {

    private UUID id;
    private UUID memberId;
    private String memberName;
    private String description;
    private BigDecimal amount;
    private BigDecimal amountPaid;
    private LocalDate paidAt;
    private PledgeStatus status;
    private UUID recordedBy;
    private LocalDateTime createdAt;

    public static PledgeResponse from(Pledge pledge) {
        return PledgeResponse.builder()
                .id(pledge.getId())
                .memberId(pledge.getMember().getId())
                .memberName(pledge.getMember().getFullName())
                .description(pledge.getDescription())
                .amount(pledge.getAmount())
                .amountPaid(pledge.getAmountPaid())
                .paidAt(pledge.getPaidAt())
                .status(pledge.getStatus())
                .recordedBy(pledge.getRecordedBy())
                .createdAt(pledge.getCreatedAt())
                .build();
    }
}
