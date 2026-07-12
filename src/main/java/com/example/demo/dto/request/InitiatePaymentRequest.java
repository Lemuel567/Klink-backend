package com.example.demo.dto.request;

import com.example.demo.model.OnlinePaymentType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class InitiatePaymentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum amount is GHS 1.00")
    @DecimalMax(value = "50000.00", message = "Maximum amount is GHS 50,000.00 per transaction")
    private BigDecimal amount;

    @NotNull(message = "Payment type is required")
    private OnlinePaymentType paymentType;

    @Size(max = 255)
    private String description;

    /** Required when paymentType is PROJECT_CONTRIBUTION. */
    private UUID projectId;
}
