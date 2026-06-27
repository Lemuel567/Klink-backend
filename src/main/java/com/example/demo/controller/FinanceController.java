package com.example.demo.controller;

import com.example.demo.dto.request.RecordOfferingRequest;
import com.example.demo.dto.request.RecordTitheRequest;
import com.example.demo.dto.request.RecordWelfareRequest;
import com.example.demo.dto.response.MemberResponse;
import com.example.demo.dto.response.PaymentResponse;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finances")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    @PostMapping("/offering")
    public ResponseEntity<PaymentResponse> recordOffering(
            @Valid @RequestBody RecordOfferingRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(financeService.recordOffering(request, principal));
    }

    @PostMapping("/tithe")
    public ResponseEntity<PaymentResponse> recordTithe(
            @Valid @RequestBody RecordTitheRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(financeService.recordTithe(request, principal));
    }

    @PostMapping("/welfare")
    public ResponseEntity<List<PaymentResponse>> recordWelfare(
            @Valid @RequestBody RecordWelfareRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(financeService.recordWelfare(request, principal));
    }

    @GetMapping("/welfare/defaulters")
    public ResponseEntity<Page<MemberResponse>> getWelfareDefaulters(
            @RequestParam String month,
            Authentication authentication,
            @PageableDefault(size = 50) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(financeService.getWelfareDefaulters(month, principal, pageable));
    }

    @PostMapping("/welfare/remind")
    public ResponseEntity<Void> sendManualWelfareReminder(
            @RequestParam String month,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        financeService.sendManualWelfareReminder(month, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<Page<PaymentResponse>> getMyFinances(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(financeService.getMyFinances(principal, pageable));
    }
}
