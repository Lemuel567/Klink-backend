package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class CreateGroupRequest {

    @NotBlank
    private String groupName;
    private String description;

    @Positive
    private BigDecimal duesAmount;

    private UUID groupAdminId;
    private UUID groupFinSecId;
}
