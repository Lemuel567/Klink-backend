package com.example.demo.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class InitiatePaymentResponse {
    private String authorizationUrl;
    private String reference;
    private BigDecimal amount;
    private String currency;
}
