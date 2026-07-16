package com.example.demo.controller;

import com.example.demo.dto.request.AssignRoleRequest;
import com.example.demo.dto.request.RegisterFcmTokenRequest;
import com.example.demo.dto.request.RegisterNonSmartphoneMemberRequest;
import com.example.demo.dto.request.UpdateMemberRequest;
import com.example.demo.dto.request.UpdatePhoneRequest;
import com.example.demo.dto.response.MessageResponse;
import com.example.demo.dto.response.MemberResponse;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/register")
    public ResponseEntity<MemberResponse> registerNonSmartphoneMember(
            @Valid @RequestBody RegisterNonSmartphoneMemberRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(memberService.registerNonSmartphoneMember(request, principal));
    }

    @GetMapping
    public ResponseEntity<Page<MemberResponse>> getAllMembers(
            Authentication authentication,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(memberService.getAllMembers(principal, search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemberResponse> getMember(
            @PathVariable UUID id,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(memberService.getMember(id, principal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemberResponse> updateMember(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMemberRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(memberService.updateMember(id, request, principal));
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<String> getQrCode(
            @PathVariable UUID id,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(memberService.getQrCode(id, principal));
    }

    // POST /api/v1/members/me/leave — a member removes THEMSELVES from the church.
    // Declared before /{id}/... routes purely for readability; "me" segments never
    // clash with UUID path variables anyway (UUID.fromString would 400 on "me").
    @PostMapping("/me/leave")
    public ResponseEntity<Void> leaveChurch(Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        memberService.leaveChurch(principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateMember(
            @PathVariable UUID id,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        memberService.deactivateMember(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivateMember(
            @PathVariable UUID id,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        memberService.reactivateMember(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/photo")
    public ResponseEntity<String> uploadPhoto(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(memberService.uploadMemberPhoto(id, file, principal));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<MemberResponse> assignRole(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRoleRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(memberService.assignRole(id, request, principal));
    }

    @PutMapping("/me/fcm-token")
    public ResponseEntity<MessageResponse> registerFcmToken(
            @Valid @RequestBody RegisterFcmTokenRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(memberService.registerFcmToken(request.getToken(), principal));
    }

    @DeleteMapping("/me/fcm-token")
    public ResponseEntity<MessageResponse> clearFcmToken(Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(memberService.clearFcmToken(principal));
    }

    @PatchMapping("/me/phone")
    public ResponseEntity<MessageResponse> updatePhone(
            @Valid @RequestBody UpdatePhoneRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(memberService.updatePhone(request, principal, getIp(httpRequest)));
    }

    private String getIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
