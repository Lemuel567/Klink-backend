package com.example.demo.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class DuesStatusResponse {

    private UUID memberId;
    private String memberName;
    private boolean paid;
    private BigDecimal amountPaid;
}
