package com.example.demo.service;

import com.example.demo.dto.request.*;
import com.example.demo.dto.response.AuthResponse;
import com.example.demo.dto.response.MessageResponse;
import com.example.demo.event.SmsVerificationEvent;
import com.example.demo.event.VerificationEmailEvent;
import com.example.demo.model.*;
import com.example.demo.repository.ChurchRepository;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.repository.VerificationTokenRepository;
import com.example.demo.security.JwtUtil;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.security.RateLimiterService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final ChurchRepository churchRepository;
    private final MemberRepository memberRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogService auditLog;
    private final RateLimiterService rateLimiterService;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private static final String INVALID_CREDENTIALS = "Invalid credentials";

    private String dummyHash;

    @PostConstruct
    void init() {
        dummyHash = passwordEncoder.encode("_klink_dummy_constant_time_hash_2025_");
    }

    public MessageResponse registerChurch(RegisterChurchRequest request, String ip) {
        java.util.Optional<Member> existing = memberRepository.findByEmail(request.getPastorEmail());

        if (existing.isEmpty()) {
            String churchCode = generateChurchCode();

            Church church = Church.builder()
                    .churchName(request.getChurchName())
                    .location(request.getLocation())
                    .denomination(request.getDenomination())
                    .contactPhone(request.getContactPhone())
                    .contactEmail(request.getContactEmail())
                    .churchCode(churchCode)
                    .build();
            church = churchRepository.save(church);

            Member pastor = Member.builder()
                    .church(church)
                    .fullName(request.getPastorName())
                    .email(request.getPastorEmail())
                    .password(passwordEncoder.encode(request.getPastorPassword()))
                    .phone(request.getPastorPhone())
                    .role(Role.PASTOR)
                    .hasSmartphone(true)
                    .qrCodeValue(UUID.randomUUID().toString())
                    .status(MemberStatus.ACTIVE)
                    .emailVerified(false)
                    .build();
            pastor = memberRepository.save(pastor);

            publishVerificationEmail(pastor, false);
            auditLog.registrationSuccess(pastor.getId(), pastor.getEmail(), ip);
        } else if (!Boolean.TRUE.equals(existing.get().getEmailVerified())) {
            // A retried registration that never got verified must resend the code —
            // silence here left users waiting for an email that would never come.
            publishVerificationEmail(existing.get(), false);
        }
        return new MessageResponse("Account created. Please check your email to verify your address before logging in.");
    }

    public MessageResponse registerMember(RegisterMemberRequest request, String ip) {
        Church church = churchRepository.findByChurchCode(request.getChurchCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid church code"));

        boolean hasEmail = request.getEmail() != null && !request.getEmail().isBlank();

        if (hasEmail) {
            java.util.Optional<Member> existingByEmail = memberRepository.findByEmail(request.getEmail());
            if (existingByEmail.isEmpty()) {
                // Keep the E.164 phone when one was provided alongside the email —
                // dropping it silently made phone login impossible for these members.
                // Skip it only if another account already owns that number.
                String phoneNumber = request.getPhoneNumber();
                boolean phoneUsable = phoneNumber != null && !phoneNumber.isBlank()
                        && memberRepository.findByPhoneNumber(phoneNumber).isEmpty();
                Member member = Member.builder()
                        .church(church)
                        .fullName(request.getFullName())
                        .email(request.getEmail())
                        .phoneNumber(phoneUsable ? phoneNumber : null)
                        .password(passwordEncoder.encode(request.getPassword()))
                        .phone(request.getPhone())
                        .dateOfBirth(request.getDateOfBirth())
                        .category(request.getCategory())
                        .role(Role.MEMBER)
                        .hasSmartphone(true)
                        .qrCodeValue(UUID.randomUUID().toString())
                        .status(MemberStatus.ACTIVE)
                        .emailVerified(false)
                        .build();
                member = memberRepository.save(member);
                publishVerificationEmail(member, false);
                auditLog.registrationSuccess(member.getId(), member.getEmail(), ip);
            } else if (!Boolean.TRUE.equals(existingByEmail.get().getEmailVerified())) {
                publishVerificationEmail(existingByEmail.get(), false);
            }
            return new MessageResponse("Account created. Please check your email to verify your address before logging in.");
        } else {
            String phoneNumber = request.getPhoneNumber();
            java.util.Optional<Member> existingByPhone = memberRepository.findByPhoneNumber(phoneNumber);
            if (existingByPhone.isEmpty()) {
                Member member = Member.builder()
                        .church(church)
                        .fullName(request.getFullName())
                        .phoneNumber(phoneNumber)
                        .password(passwordEncoder.encode(request.getPassword()))
                        .phone(request.getPhone())
                        .dateOfBirth(request.getDateOfBirth())
                        .category(request.getCategory())
                        .role(Role.MEMBER)
                        .hasSmartphone(true)
                        .qrCodeValue(UUID.randomUUID().toString())
                        .status(MemberStatus.ACTIVE)
                        .phoneVerified(false)
                        .build();
                member = memberRepository.save(member);
                publishVerificationSms(member);
                auditLog.phoneVerificationSent(member.getId(), member.getPhoneNumber(), ip);
            } else if (!Boolean.TRUE.equals(existingByPhone.get().getPhoneVerified())) {
                // Same trap as the email path: a retried, never-verified signup
                // must get a fresh SMS code instead of nothing.
                publishVerificationSms(existingByPhone.get());
                auditLog.phoneVerificationSent(existingByPhone.get().getId(), phoneNumber, ip);
            }
            return new MessageResponse("Account created. A verification code has been sent to your phone.");
        }
    }

    public AuthResponse login(LoginRequest request, String ip) {
        boolean hasEmail = request.getEmail() != null && !request.getEmail().isBlank();
        String identifier = hasEmail ? request.getEmail() : request.getPhoneNumber();

        if (rateLimiterService.isLoginLimitExhausted(identifier, ip)) {
            long retryMins = rateLimiterService.loginRetryAfterSeconds() / 60;
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many login attempts. Try again in " + retryMins + " minutes.");
        }

        Member member = hasEmail
                ? memberRepository.findByEmail(request.getEmail()).orElse(null)
                : memberRepository.findByPhoneNumber(request.getPhoneNumber()).orElse(null);

        String hashToCheck = (member != null) ? member.getPassword() : dummyHash;
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), hashToCheck);

        if (member == null) {
            rateLimiterService.recordLoginFailure(identifier, ip);
            auditLog.loginFailure(maskIdentifier(hasEmail, identifier), ip);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }

        boolean lockoutExpired = member.getLockedUntil() != null
                && !member.getLockedUntil().isAfter(LocalDateTime.now());
        if (lockoutExpired) {
            memberRepository.resetLoginAttempts(member.getId());
        }

        boolean inactivityReset = !lockoutExpired
                && member.getFailedLoginAttempts() > 0
                && member.getLastFailedAt() != null
                && member.getLastFailedAt().isBefore(
                        LocalDateTime.now().minusHours(rateLimiterService.getLoginFailureResetHours()));
        if (inactivityReset) {
            memberRepository.resetLoginAttempts(member.getId());
        }

        boolean stillLocked = !lockoutExpired
                && member.getLockedUntil() != null
                && member.getLockedUntil().isAfter(LocalDateTime.now());
        if (stillLocked) {
            auditLog.accountLocked(maskIdentifier(hasEmail, identifier), ip);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }

        if (!passwordMatches) {
            memberRepository.incrementFailedLoginAttempts(member.getId(), LocalDateTime.now());
            int newCount = memberRepository.getFailedLoginAttempts(member.getId());
            if (newCount >= rateLimiterService.getLockoutAttempts()) {
                memberRepository.lockAccount(member.getId(),
                        LocalDateTime.now().plusMinutes(rateLimiterService.getLockoutMinutes()));
                auditLog.accountLocked(maskIdentifier(hasEmail, identifier), ip);
            }
            rateLimiterService.recordLoginFailure(identifier, ip);
            auditLog.loginFailure(maskIdentifier(hasEmail, identifier), ip);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }

        if (member.getStatus() == MemberStatus.DEACTIVATED) {
            rateLimiterService.recordLoginFailure(identifier, ip);
            auditLog.loginFailure(maskIdentifier(hasEmail, identifier), ip);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }

        boolean isVerified = Boolean.TRUE.equals(member.getEmailVerified())
                || Boolean.TRUE.equals(member.getPhoneVerified());
        if (!isVerified) {
            auditLog.loginFailure(maskIdentifier(hasEmail, identifier), ip);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }

        // Members of a soft-deleted church cannot log in — except an Elder, who needs a
        // token to call POST /church/restore during the 30-day grace period.
        if (member.getChurch().getDeletedAt() != null && member.getRole() != Role.ELDER) {
            auditLog.loginFailure(maskIdentifier(hasEmail, identifier), ip);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This church is scheduled for deletion on "
                            + member.getChurch().getDeletedAt().plusDays(30).toLocalDate()
                            + ". Contact an Elder to restore it.");
        }

        // Extract church fields before resetLoginAttempts clears the PC (clearAutomatically=true detaches member)
        UUID churchId = member.getChurch().getId();
        String churchCode = member.getChurch().getChurchCode();

        memberRepository.resetLoginAttempts(member.getId());
        String accessToken = jwtUtil.generateToken(member.getId(), churchId, member.getEmail(), member.getRole(), member.getTokenVersion());
        String refreshToken = issueRefreshToken(member.getId(), UUID.randomUUID());

        if (hasEmail) {
            auditLog.loginSuccess(member.getId(), member.getEmail(), ip);
        } else {
            auditLog.loginViaPhone(member.getId(), member.getPhoneNumber(), ip);
        }

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .memberId(member.getId())
                .churchId(churchId)
                .churchCode(churchCode)
                .role(member.getRole())
                .fullName(member.getFullName())
                .email(member.getEmail())
                .photoUrl(member.getPhotoUrl())
                .emailVerified(Boolean.TRUE.equals(member.getEmailVerified()))
                .phoneVerified(Boolean.TRUE.equals(member.getPhoneVerified()))
                .build();
    }

    public AuthResponse refresh(RefreshTokenRequest request, String ip) {
        String tokenHash = sha256(request.getRefreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired. Please log in again."));

        if (stored.isRevoked()) {
            refreshTokenRepository.revokeAllInFamily(stored.getFamilyId());
            auditLog.tokenReuseDetected(stored.getMemberId(), ip);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Session invalidated due to suspicious activity. Please log in again.");
        }

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired. Please log in again.");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        Member member = memberRepository.findById(stored.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS));

        if (member.getStatus() == MemberStatus.DEACTIVATED) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }

        UUID churchId = member.getChurch().getId();
        String churchCode = member.getChurch().getChurchCode();
        String newAccessToken = jwtUtil.generateToken(member.getId(), churchId, member.getEmail(), member.getRole(), member.getTokenVersion());
        String newRefreshToken = issueRefreshToken(member.getId(), stored.getFamilyId());

        auditLog.tokenRefreshed(member.getId(), ip);

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .memberId(member.getId())
                .churchId(churchId)
                .churchCode(churchCode)
                .role(member.getRole())
                .fullName(member.getFullName())
                .email(member.getEmail())
                .photoUrl(member.getPhotoUrl())
                .emailVerified(Boolean.TRUE.equals(member.getEmailVerified()))
                .phoneVerified(Boolean.TRUE.equals(member.getPhoneVerified()))
                .build();
    }

    public void logout(MemberPrincipal principal, String ip) {
        Member member = memberRepository.findByChurchIdAndId(principal.getChurchId(), principal.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        member.setTokenVersion(member.getTokenVersion() + 1);
        memberRepository.save(member);
        refreshTokenRepository.revokeAllForMember(principal.getMemberId());
        auditLog.logout(principal.getMemberId(), ip);
    }

    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        if (rateLimiterService.checkVerifyEmail(request.getEmail())) {
            long retryMins = rateLimiterService.verifyEmailRetryAfterSeconds() / 60;
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many verification attempts. Try again in " + retryMins + " minutes.");
        }

        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code"));

        if (Boolean.TRUE.equals(member.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already verified");
        }

        String codeHash = sha256(request.getCode());

        VerificationToken vt = verificationTokenRepository
                .findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(member.getEmail(), VerificationTokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code"));

        if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code has expired. Request a new one.");
        }

        if (!vt.getCode().equals(codeHash)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code");
        }

        vt.setUsed(true);
        verificationTokenRepository.save(vt);

        member.setEmailVerified(true);
        memberRepository.save(member);

        UUID churchId = member.getChurch().getId();
        String churchCode = member.getChurch().getChurchCode();
        String accessToken = jwtUtil.generateToken(member.getId(), churchId, member.getEmail(), member.getRole(), member.getTokenVersion());
        String refreshToken = issueRefreshToken(member.getId(), UUID.randomUUID());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .memberId(member.getId())
                .churchId(churchId)
                .churchCode(churchCode)
                .role(member.getRole())
                .fullName(member.getFullName())
                .email(member.getEmail())
                .photoUrl(member.getPhotoUrl())
                .emailVerified(true)
                .phoneVerified(Boolean.TRUE.equals(member.getPhoneVerified()))
                .build();
    }

    public MessageResponse resendVerificationCode(ResendVerificationRequest request) {
        memberRepository.findByEmail(request.getEmail()).ifPresent(member -> {
            if (!Boolean.TRUE.equals(member.getEmailVerified())) {
                publishVerificationEmail(member, false);
            }
        });
        return new MessageResponse("If that email is registered and unverified, a new code has been sent.");
    }

    public AuthResponse verifyPhone(VerifyPhoneRequest request) {
        String phoneNumber = request.getPhoneNumber();

        if (rateLimiterService.checkVerifyPhone(phoneNumber)) {
            long retryMins = rateLimiterService.verifyPhoneRetryAfterSeconds() / 60;
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many verification attempts. Try again in " + retryMins + " minutes.");
        }

        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code"));

        if (Boolean.TRUE.equals(member.getPhoneVerified())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is already verified");
        }

        if (member.getPhoneVerificationCodeHash() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code");
        }

        if (member.getPhoneVerificationCodeExpiresAt() != null
                && member.getPhoneVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            auditLog.phoneVerificationExpired(member.getId(), phoneNumber);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code has expired. Request a new one.");
        }

        String codeHash = sha256(request.getCode());
        if (!codeHash.equals(member.getPhoneVerificationCodeHash())) {
            // Atomic increment: returns 0 if already at the limit (concurrent-safe).
            int updated = memberRepository.incrementPhoneVerificationAttemptsIfUnderLimit(
                    member.getId(), LocalDateTime.now(), 5);
            if (updated == 0) {
                auditLog.phoneVerificationLocked(member.getId(), phoneNumber);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many failed attempts. Request a new code.");
            }
            auditLog.phoneVerificationFailed(member.getId(), phoneNumber);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code");
        }

        member.setPhoneVerified(true);
        member.setPhoneVerificationCodeHash(null);
        member.setPhoneVerificationCodeExpiresAt(null);
        member.setPhoneVerificationAttempts(0);
        member.setLastPhoneVerificationAttemptAt(null);
        memberRepository.save(member);

        auditLog.phoneVerificationSuccess(member.getId(), phoneNumber);

        UUID churchId = member.getChurch().getId();
        String churchCode = member.getChurch().getChurchCode();
        String accessToken = jwtUtil.generateToken(member.getId(), churchId, member.getEmail(), member.getRole(), member.getTokenVersion());
        String refreshToken = issueRefreshToken(member.getId(), UUID.randomUUID());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .memberId(member.getId())
                .churchId(churchId)
                .churchCode(churchCode)
                .role(member.getRole())
                .fullName(member.getFullName())
                .email(member.getEmail())
                .photoUrl(member.getPhotoUrl())
                .emailVerified(Boolean.TRUE.equals(member.getEmailVerified()))
                .phoneVerified(true)
                .build();
    }

    public MessageResponse resendPhoneVerification(ResendPhoneVerificationRequest request) {
        if (rateLimiterService.checkResendPhoneVerification(request.getPhoneNumber())) {
            long retryMins = rateLimiterService.phoneResendRetryAfterSeconds() / 60;
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many resend attempts. Try again in " + retryMins + " minutes.");
        }
        memberRepository.findByPhoneNumber(request.getPhoneNumber()).ifPresent(member -> {
            if (!Boolean.TRUE.equals(member.getPhoneVerified())) {
                publishVerificationSms(member);
            }
        });
        return new MessageResponse("If that phone number is registered and unverified, a new code has been sent.");
    }

    public MessageResponse forgotPassword(ForgotPasswordRequest request, String ip) {
        memberRepository.findByEmail(request.getEmail()).ifPresent(member -> {
            String rawCode = generateSixDigitCode();
            String codeHash = sha256(rawCode);

            verificationTokenRepository.markAllUnusedAsUsed(member.getEmail(), VerificationTokenType.PASSWORD_RESET);

            VerificationToken vt = VerificationToken.builder()
                    .email(member.getEmail())
                    .code(codeHash)
                    .type(VerificationTokenType.PASSWORD_RESET)
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .used(false)
                    .build();
            verificationTokenRepository.save(vt);
            eventPublisher.publishEvent(new VerificationEmailEvent(this, member.getEmail(), member.getFullName(), rawCode, true));
        });
        auditLog.passwordReset(request.getEmail(), ip);
        return new MessageResponse("If that email is registered, a password reset code has been sent.");
    }

    public void resetPassword(ResetPasswordRequest request, String ip) {
        if (rateLimiterService.checkResetPassword(request.getEmail())) {
            long retryMins = rateLimiterService.resetPasswordRetryAfterSeconds() / 60;
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many reset attempts. Try again in " + retryMins + " minutes.");
        }

        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code or email"));

        VerificationToken vt = verificationTokenRepository
                .findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(request.getEmail(), VerificationTokenType.PASSWORD_RESET)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired code"));

        if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code has expired. Request a new one.");
        }

        String codeHash = sha256(request.getCode());
        if (!vt.getCode().equals(codeHash)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code");
        }

        vt.setUsed(true);
        verificationTokenRepository.save(vt);

        member.setPassword(passwordEncoder.encode(request.getNewPassword()));
        member.setPasswordChangedAt(LocalDateTime.now());
        member.setTokenVersion(member.getTokenVersion() + 1);
        memberRepository.save(member);
        refreshTokenRepository.revokeAllForMember(member.getId());

        auditLog.passwordReset(member.getEmail(), ip);
    }

    public void changePassword(ChangePasswordRequest request, MemberPrincipal principal, String ip) {
        Member member = memberRepository.findByChurchIdAndId(principal.getChurchId(), principal.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        member.setPassword(passwordEncoder.encode(request.getNewPassword()));
        member.setPasswordChangedAt(LocalDateTime.now());
        member.setTokenVersion(member.getTokenVersion() + 1);
        memberRepository.save(member);
        refreshTokenRepository.revokeAllForMember(principal.getMemberId());

        auditLog.passwordChanged(principal.getMemberId(), ip);
    }

    private String issueRefreshToken(UUID memberId, UUID familyId) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String hash = sha256(rawToken);

        RefreshToken rt = RefreshToken.builder()
                .memberId(memberId)
                .familyId(familyId)
                .tokenHash(hash)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .build();
        refreshTokenRepository.save(rt);
        return rawToken;
    }

    private void publishVerificationEmail(Member member, boolean passwordReset) {
        String rawCode = generateSixDigitCode();
        String codeHash = sha256(rawCode);

        verificationTokenRepository.markAllUnusedAsUsed(member.getEmail(), VerificationTokenType.EMAIL_VERIFICATION);

        VerificationToken vt = VerificationToken.builder()
                .email(member.getEmail())
                .code(codeHash)
                .type(VerificationTokenType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();
        verificationTokenRepository.save(vt);
        eventPublisher.publishEvent(new VerificationEmailEvent(this, member.getEmail(), member.getFullName(), rawCode, passwordReset));
    }

    private void publishVerificationSms(Member member) {
        String rawCode = generateSixDigitCode();
        String codeHash = sha256(rawCode);

        member.setPhoneVerificationCodeHash(codeHash);
        member.setPhoneVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
        member.setPhoneVerificationAttempts(0);
        memberRepository.save(member);

        // Published after transaction commits — SMS send is async and never inside the transaction
        eventPublisher.publishEvent(new SmsVerificationEvent(this, member.getPhoneNumber(), member.getFullName(), rawCode));
    }

    String sha256(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes());
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    String generateSixDigitCode() {
        SecureRandom random = new SecureRandom();
        String code;
        do {
            int n = 100000 + random.nextInt(900000);
            code = String.valueOf(n);
        } while (isWeakCode(code));
        return code;
    }

    private boolean isWeakCode(String code) {
        if (code.chars().distinct().count() == 1) return true;
        boolean ascending = true, descending = true;
        for (int i = 1; i < code.length(); i++) {
            int diff = (code.charAt(i) - '0') - (code.charAt(i - 1) - '0');
            if (diff != 1) ascending = false;
            if (diff != -1) descending = false;
        }
        return ascending || descending;
    }

    private String maskIdentifier(boolean hasEmail, String identifier) {
        if (hasEmail) return identifier;
        if (identifier == null || identifier.length() < 4) return "****";
        return "****" + identifier.substring(identifier.length() - 4);
    }

    private String generateChurchCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        String code;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            code = sb.toString();
        } while (churchRepository.existsByChurchCode(code));
        return code;
    }
}
