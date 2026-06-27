package com.example.demo.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class GenerateAttendanceQrResponse {

    private String qrPayload;
    private String serviceName;
    private LocalDate serviceDate;
}
