package com.example.demo.controller;

import com.example.demo.dto.request.RecordPledgePaymentRequest;
import com.example.demo.dto.request.RecordPledgeRequest;
import com.example.demo.dto.response.PledgePaymentResponse;
import com.example.demo.dto.response.PledgeResponse;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.PledgeService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pledges")
@RequiredArgsConstructor
public class PledgeController {

    private final PledgeService pledgeService;

    @PostMapping
    public ResponseEntity<PledgeResponse> recordPledge(
            @Valid @RequestBody RecordPledgeRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pledgeService.recordPledge(request, principal));
    }

    @GetMapping
    public ResponseEntity<Page<PledgeResponse>> getAllPledges(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(pledgeService.getAllPledges(principal, pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<Page<PledgeResponse>> getMyPledges(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(pledgeService.getMyPledges(principal, pageable));
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<PledgePaymentResponse> payPledge(
            @PathVariable UUID id,
            @Valid @RequestBody RecordPledgePaymentRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pledgeService.payPledge(id, request, principal));
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<List<PledgePaymentResponse>> getPledgePayments(
            @PathVariable UUID id,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(pledgeService.getPledgePayments(id, principal));
    }
}
