package com.example.demo.dto.request;

import com.example.demo.model.ProjectType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class UpdateProjectRequest {

    @Size(min = 1, max = 300)
    private String title;

    @Size(min = 1, max = 5000)
    private String description;

    private ProjectType projectType;

    @DecimalMin(value = "0.01")
    private BigDecimal targetAmount;

    @Size(max = 10)
    private String currency;

    private LocalDate startDate;
    private LocalDate expectedEndDate;
    // actualEndDate intentionally absent — it is set only by COMPLETED/CANCELLED status transitions

    @Size(max = 500)
    private String location;

    @Size(max = 300)
    private String contractor;

    private UUID facilityId;

    private Boolean isPublic;
}