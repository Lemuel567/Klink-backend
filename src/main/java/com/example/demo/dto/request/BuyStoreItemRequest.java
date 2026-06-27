package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "Mobile Money reference is required")
    @Size(max = 100, message = "MoMo reference must not exceed 100 characters")
    private String momoReference;
}