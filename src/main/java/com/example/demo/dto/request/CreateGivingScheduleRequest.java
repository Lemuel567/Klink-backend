package com.example.demo.dto.request;

import com.example.demo.model.OnlinePaymentType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateGivingScheduleRequest {

    @NotNull
    private OnlinePaymentType paymentType;

    @NotNull
    @DecimalMin(value = "1.0", message = "Amount must be at least 1")
    @DecimalMax(value = "50000.0", message = "Amount must be at most 50,000")
    private BigDecimal amount;

    @Min(value = 1, message = "Day must be between 1 and 28")
    @Max(value = 28, message = "Day must be between 1 and 28")
    private int dayOfMonth;
}
