package com.example.demo.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class RecordOfferingRequest {

    @NotNull
    private LocalDate serviceDate;

    @NotNull @Positive
    private BigDecimal amount;
}
