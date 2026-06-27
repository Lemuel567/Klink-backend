package com.example.demo.controller;

import com.example.demo.dto.request.PostDevotionalRequest;
import com.example.demo.dto.response.DevotionalResponse;
import java.util.UUID;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.DevotionalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/devotionals")
@RequiredArgsConstructor
public class DevotionalController {

    private final DevotionalService devotionalService;

    @PostMapping
    public ResponseEntity<DevotionalResponse> postDevotional(
            @Valid @RequestBody PostDevotionalRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(devotionalService.postDevotional(request, principal));
    }

    @GetMapping
    public ResponseEntity<Page<DevotionalResponse>> getAllDevotionals(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(devotionalService.getAllDevotionals(principal, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevotional(
            @PathVariable UUID id,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        devotionalService.deleteDevotional(id, principal);
        return ResponseEntity.noContent().build();
    }
}
