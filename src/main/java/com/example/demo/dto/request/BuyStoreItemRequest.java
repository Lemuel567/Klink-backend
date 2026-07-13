package com.example.demo.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class BuyStoreItemRequest {

    @NotNull(message = "Item ID is required")
    private UUID itemId;

    private LocalDate datePaid;

    // Required for member self-purchases (enforced in StoreService) — optional
    // for a FinSec-recorded cash sale, where it may hold a receipt number.
    @Size(max = 100, message = "Payment reference must not exceed 100 characters")
    private String momoReference;

    /**
     * Optional (2026-07-12): a FINANCIAL_SECRETARY records an offline/cash sale
     * ON BEHALF OF this member. Null or non-FinSec caller → buyer is the caller.
     */
    private UUID memberId;
}