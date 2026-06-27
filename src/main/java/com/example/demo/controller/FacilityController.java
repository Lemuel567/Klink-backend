package com.example.demo.controller;

import com.example.demo.dto.request.AddFacilityImageRequest;
import com.example.demo.dto.request.CreateFacilityRequest;
import com.example.demo.dto.request.UpdateFacilityRequest;
import com.example.demo.dto.response.FacilityDetailResponse;
import com.example.demo.dto.response.FacilityImageResponse;
import com.example.demo.dto.response.FacilityResponse;
import com.example.demo.model.FacilityCondition;
import com.example.demo.model.FacilityType;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.FacilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/facilities")
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityService facilityService;

    @PostMapping
    public ResponseEntity<FacilityResponse> create(
            @Valid @RequestBody CreateFacilityRequest request,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facilityService.createFacility(request, principal));
    }

    @GetMapping
    public ResponseEntity<Page<FacilityResponse>> list(
            @RequestParam(required = false) FacilityType facilityType,
            @RequestParam(required = false) FacilityCondition condition,
            @RequestParam(required = false) Boolean isActive,
            @AuthenticationPrincipal MemberPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(facilityService.listFacilities(principal, facilityType, condition, isActive, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacilityDetailResponse> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(facilityService.getFacility(id, principal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FacilityResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFacilityRequest request,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(facilityService.updateFacility(id, request, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal MemberPrincipal principal) {
        facilityService.deleteFacility(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<FacilityImageResponse> addImage(
            @PathVariable UUID id,
            @Valid @RequestBody AddFacilityImageRequest request,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facilityService.addImage(id, request, principal));
    }

    @GetMapping("/{id}/images")
    public ResponseEntity<List<FacilityImageResponse>> getImages(
            @PathVariable UUID id,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(facilityService.getImages(id, principal));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId,
            @AuthenticationPrincipal MemberPrincipal principal) {
        facilityService.deleteImage(id, imageId, principal);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/images/{imageId}/primary")
    public ResponseEntity<FacilityImageResponse> setPrimary(
            @PathVariable UUID id,
            @PathVariable UUID imageId,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(facilityService.setPrimaryImage(id, imageId, principal));
    }
}