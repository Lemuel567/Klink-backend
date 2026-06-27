package com.example.demo.dto.response;

import com.example.demo.model.Payment;
import com.example.demo.model.PaymentStatus;
import com.example.demo.model.PaymentType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class PaymentResponse {

    private UUID id;
    private PaymentType paymentType;
    private PaymentStatus status;
    private BigDecimal amount;
    private String paymentMonth;
    private LocalDate paymentDate;
    private UUID memberId;
    private String memberName;
    private UUID recordedBy;
    private LocalDateTime createdAt;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentType(payment.getPaymentType())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .paymentMonth(payment.getPaymentMonth())
                .paymentDate(payment.getPaymentDate())
                .memberId(payment.getMember() != null ? payment.getMember().getId() : null)
                .memberName(payment.getMember() != null ? payment.getMember().getFullName() : null)
                .recordedBy(payment.getRecordedBy())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
