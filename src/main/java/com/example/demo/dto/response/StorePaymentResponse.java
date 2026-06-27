package com.example.demo.dto.response;

import com.example.demo.model.CollectionStatus;
import com.example.demo.model.StorePayment;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class StorePaymentResponse {

    private UUID id;
    private UUID memberId;
    private String memberName;
    private UUID itemId;
    private String itemName;
    private BigDecimal amount;
    private LocalDate datePaid;
    private CollectionStatus collectionStatus;
    private UUID collectedBy;
    private LocalDateTime collectedAt;

    public static StorePaymentResponse from(StorePayment payment) {
        return StorePaymentResponse.builder()
                .id(payment.getId())
                .memberId(payment.getMember().getId())
                .memberName(payment.getMember().getFullName())
                .itemId(payment.getItem().getId())
                .itemName(payment.getItem().getName())
                .amount(payment.getAmount())
                .datePaid(payment.getDatePaid())
                .collectionStatus(payment.getCollectionStatus())
                .collectedBy(payment.getCollectedBy())
                .collectedAt(payment.getCollectedAt())
                .build();
    }
}
