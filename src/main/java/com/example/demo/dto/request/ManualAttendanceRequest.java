package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class ManualAttendanceRequest {

    @NotNull(message = "Member ID is required")
    private UUID memberId;

    @NotBlank(message = "Service name is required")
    @Size(max = 200, message = "Service name must not exceed 200 characters")
    private String serviceName;

    @NotNull(message = "Service date is required")
    @PastOrPresent(message = "Service date cannot be in the future")
    private LocalDate serviceDate;
}