package com.example.demo.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class GenerateAttendanceQrRequest {

    private String serviceName;
    private LocalDate serviceDate;
}
