package com.example.demo.controller;

import com.example.demo.dto.request.CreatePrayerRequestRequest;
import com.example.demo.dto.request.RespondPrayerRequestRequest;
import com.example.demo.dto.response.PrayerRequestResponse;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.PrayerRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prayer-requests")
@RequiredArgsConstructor
public class PrayerRequestController {

    private final PrayerRequestService prayerRequestService;

    // POST /api/v1/prayer-requests — any member submits a request
    @PostMapping
    public ResponseEntity<PrayerRequestResponse> create(
            @Valid @RequestBody CreatePrayerRequestRequest request,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(prayerRequestService.createPrayerRequest(request, principal));
    }

    // GET /api/v1/prayer-requests — PUBLIC + own PRIVATE for members; all for leaders
    @GetMapping
    public ResponseEntity<Page<PrayerRequestResponse>> list(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(prayerRequestService.getPrayerRequests(principal, pageable));
    }

    // GET /api/v1/prayer-requests/{id}
    @GetMapping("/{id}")
    public ResponseEntity<PrayerRequestResponse> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(prayerRequestService.getPrayerRequest(id, principal));
    }

    // PUT /api/v1/prayer-requests/{id}/respond — Pastor/Elder responds + marks answered
    @PutMapping("/{id}/respond")
    public ResponseEntity<PrayerRequestResponse> respond(
            @PathVariable UUID id,
            @Valid @RequestBody RespondPrayerRequestRequest request,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(prayerRequestService.respond(id, request, principal));
    }

    // DELETE /api/v1/prayer-requests/{id} — author or Pastor/Elder (soft delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal MemberPrincipal principal) {
        prayerRequestService.deletePrayerRequest(id, principal);
        return ResponseEntity.noContent().build();
    }
}
