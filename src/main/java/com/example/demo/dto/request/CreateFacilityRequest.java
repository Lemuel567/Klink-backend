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
public class CreateFacilityRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull(message = "Facility type is required")
    private FacilityType facilityType;

    @Size(max = 500)
    private String address;

    @Min(0)
    private Integer capacity;

    @Min(1800) @Max(2100)
    private Integer yearAcquired;

    @DecimalMin(value = "0.00", message = "Estimated value cannot be negative")
    private BigDecimal estimatedValue;

    @Size(max = 10)
    private String currency;

    @NotNull(message = "Condition is required")
    private FacilityCondition condition;

    @Size(max = 2000)
    private String notes;
}