package com.example.demo.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Patch-style update for a group. All fields optional; only non-null fields are applied.
 */
@Getter
@NoArgsConstructor
public class UpdateGroupRequest {

    private String groupName;
    private String description;

    @Positive
    private BigDecimal duesAmount;
}
