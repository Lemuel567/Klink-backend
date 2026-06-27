package com.example.demo.dto.response;

import com.example.demo.model.Attendance;
import com.example.demo.model.AttendanceMethod;
import com.example.demo.model.AttendanceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AttendanceResponse {

    private UUID id;
    private UUID memberId;
    private String memberName;
    private String serviceName;
    private LocalDate serviceDate;
    private LocalDateTime timeOfScan;
    private AttendanceMethod method;
    private AttendanceStatus status;

    public static AttendanceResponse from(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .memberId(attendance.getMember().getId())
                .memberName(attendance.getMember().getFullName())
                .serviceName(attendance.getServiceName())
                .serviceDate(attendance.getServiceDate())
                .timeOfScan(attendance.getTimeOfScan())
                .method(attendance.getMethod())
                .status(attendance.getStatus())
                .build();
    }
}
