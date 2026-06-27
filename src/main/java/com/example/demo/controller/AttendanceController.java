package com.example.demo.controller;

import com.example.demo.dto.request.ManualAttendanceRequest;
import com.example.demo.dto.request.ScanAttendanceRequest;
import com.example.demo.dto.response.AttendanceResponse;
import com.example.demo.dto.response.GenerateAttendanceQrResponse;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/generate-qr")
    public ResponseEntity<GenerateAttendanceQrResponse> generateQr(Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.generateQr(principal));
    }

    @PostMapping("/scan")
    public ResponseEntity<AttendanceResponse> scan(
            @Valid @RequestBody ScanAttendanceRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.scan(request, principal));
    }

    @PostMapping("/manual")
    public ResponseEntity<AttendanceResponse> markManual(
            @Valid @RequestBody ManualAttendanceRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.markManual(request, principal));
    }

    @GetMapping
    public ResponseEntity<Page<AttendanceResponse>> getAllAttendance(
            Authentication authentication,
            @PageableDefault(size = 50) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(attendanceService.getAllAttendance(principal, pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<Page<AttendanceResponse>> getMyAttendance(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(attendanceService.getMyAttendance(principal, pageable));
    }
}
