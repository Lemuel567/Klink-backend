package com.example.demo.dto.request;

import com.example.demo.model.ProjectType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CreateProjectRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 300)
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 5000)
    private String description;

    @NotNull(message = "Project type is required")
    private ProjectType projectType;

    @NotNull(message = "Target amount is required")
    @DecimalMin(value = "0.01", message = "Target amount must be greater than zero")
    private BigDecimal targetAmount;

    @Size(max = 10)
    private String currency;

    private LocalDate startDate;

    private LocalDate expectedEndDate;

    @Size(max = 500)
    private String location;

    @Size(max = 300)
    private String contractor;

    private UUID facilityId;

    @JsonProperty("isPublic")
    private boolean isPublic = true;
}