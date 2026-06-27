package com.example.demo.dto.request;

import com.example.demo.model.ContributionPaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class RecordProjectContributionRequest {

    @NotNull(message = "Member ID is required")
    private UUID memberId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @Size(max = 10)
    private String currency;

    @NotNull(message = "Contribution date is required")
    @PastOrPresent(message = "Contribution date cannot be in the future")
    private LocalDate contributionDate;

    @NotNull(message = "Payment method is required")
    private ContributionPaymentMethod paymentMethod;

    @Size(max = 1000)
    private String notes;
}