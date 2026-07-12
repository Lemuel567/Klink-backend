package com.example.demo.dto.response;

import com.example.demo.model.OnlinePaymentStatus;
import com.example.demo.model.OnlinePaymentType;
import com.example.demo.model.PaystackTransaction;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class OnlinePaymentResponse {

    private UUID id;
    private UUID memberId;
    private String memberName;
    private BigDecimal amount;
    private String currency;
    private OnlinePaymentType paymentType;
    private OnlinePaymentStatus status;
    private String channel;
    private String paystackReference;
    private String description;
    private UUID projectId;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    public static OnlinePaymentResponse from(PaystackTransaction t, String memberName) {
        return OnlinePaymentResponse.builder()
                .id(t.getId())
                .memberId(t.getMemberId())
                .memberName(memberName)
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .paymentType(t.getPaymentType())
                .status(t.getStatus())
                .channel(t.getChannel())
                .paystackReference(t.getPaystackReference())
                .description(t.getDescription())
                .projectId(t.getProjectId())
                .paidAt(t.getPaidAt())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
