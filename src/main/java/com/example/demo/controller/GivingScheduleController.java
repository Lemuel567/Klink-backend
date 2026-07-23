package com.example.demo.controller;

import com.example.demo.dto.request.CreateGivingScheduleRequest;
import com.example.demo.dto.response.GivingScheduleResponse;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.GivingScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/giving-schedules")
@RequiredArgsConstructor
public class GivingScheduleController {

    private final GivingScheduleService service;

    @PostMapping
    public ResponseEntity<GivingScheduleResponse> create(
            @Valid @RequestBody CreateGivingScheduleRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, principal));
    }

    @GetMapping
    public ResponseEntity<List<GivingScheduleResponse>> listMine(Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(service.listMine(principal));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<GivingScheduleResponse> setActive(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        boolean active = Boolean.TRUE.equals(body.get("active"));
        return ResponseEntity.ok(service.setActive(id, active, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        service.delete(id, principal);
        return ResponseEntity.noContent().build();
    }
}
