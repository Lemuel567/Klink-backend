package com.example.demo.dto.request;

import com.example.demo.model.FacilityCondition;
import com.example.demo.model.FacilityType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class UpdateFacilityRequest {

    @Size(min = 1, max = 200)
    private String name;

    @Size(max = 2000)
    private String description;

    private FacilityType facilityType;

    @Size(max = 500)
    private String address;

    @Min(0)
    private Integer capacity;

    @Min(1800) @Max(2100)
    private Integer yearAcquired;

    @DecimalMin(value = "0.00")
    private BigDecimal estimatedValue;

    @Size(max = 10)
    private String currency;

    private FacilityCondition condition;

    private Boolean isActive;

    @Size(max = 2000)
    private String notes;
}