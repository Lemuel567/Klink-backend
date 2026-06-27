package com.example.demo.dto.response;

import com.example.demo.model.PledgePayment;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class PledgePaymentResponse {

    private UUID id;
    private UUID pledgeId;
    private UUID memberId;
    private String memberName;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private UUID recordedBy;
    private LocalDateTime createdAt;

    public static PledgePaymentResponse from(PledgePayment payment) {
        return PledgePaymentResponse.builder()
                .id(payment.getId())
                .pledgeId(payment.getPledge().getId())
                .memberId(payment.getMember().getId())
                .memberName(payment.getMember().getFullName())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .recordedBy(payment.getRecordedBy())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
