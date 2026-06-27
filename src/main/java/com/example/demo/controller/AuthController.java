package com.example.demo.controller;

import com.example.demo.dto.request.*;
import com.example.demo.dto.response.AuthResponse;
import com.example.demo.dto.response.MessageResponse;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register-church")
    public ResponseEntity<MessageResponse> registerChurch(
            @Valid @RequestBody RegisterChurchRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerChurch(request, getIp(httpRequest)));
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(
            @Valid @RequestBody RegisterMemberRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerMember(request, getIp(httpRequest)));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, getIp(httpRequest)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.refresh(request, getIp(httpRequest)));
    }

    // FIX 1: verify-email is now public — no Authentication required.
    // Tokens are issued here, not at registration.
    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    // FIX 1: resend-verification is now public — accepts email in request body.
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        return ResponseEntity.ok(authService.resendVerificationCode(request));
    }

    @PostMapping("/verify-phone")
    public ResponseEntity<AuthResponse> verifyPhone(
            @Valid @RequestBody VerifyPhoneRequest request) {
        return ResponseEntity.ok(authService.verifyPhone(request));
    }

    @PostMapping("/resend-phone-verification")
    public ResponseEntity<MessageResponse> resendPhoneVerification(
            @Valid @RequestBody ResendPhoneVerificationRequest request) {
        return ResponseEntity.ok(authService.resendPhoneVerification(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.forgotPassword(request, getIp(httpRequest)));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {
        authService.resetPassword(request, getIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        authService.changePassword(request, principal, getIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            Authentication authentication,
            HttpServletRequest httpRequest) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        authService.logout(principal, getIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    private String getIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
