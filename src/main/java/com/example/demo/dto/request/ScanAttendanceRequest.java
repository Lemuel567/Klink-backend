package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ScanAttendanceRequest {

    @NotBlank(message = "QR payload is required")
    private String qrPayload;
}