package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class RecordWelfareRequest {

    @NotNull
    private UUID memberId;

    @NotNull @Positive
    private BigDecimal amountPaid;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "paymentMonth must be in YYYY-MM format")
    private String paymentMonth;

    @NotNull
    private LocalDate paymentDate;

    private String momoReference;
}
