package com.example.demo.service;

import com.example.demo.dto.request.AssignRoleRequest;
import com.example.demo.dto.request.UpdatePhoneRequest;
import com.example.demo.dto.response.MessageResponse;
import com.example.demo.event.SmsVerificationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;
import com.example.demo.dto.request.RegisterNonSmartphoneMemberRequest;
import com.example.demo.dto.request.UpdateMemberRequest;
import com.example.demo.dto.response.MemberResponse;
import com.example.demo.model.*;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.security.RateLimiterService;
import com.example.demo.security.RoleChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private static final int MAX_MANAGERS = 10;
    private static final int MAX_ELDERS = 25;

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SupabaseStorageService storageService;
    private final RateLimiterService rateLimiterService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogService auditLog;

    public MemberResponse registerNonSmartphoneMember(RegisterNonSmartphoneMemberRequest request,
                                                       MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);

        Member member = Member.builder()
                .church(principal.getMember().getChurch())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .category(request.getCategory())
                .role(Role.MEMBER)
                .hasSmartphone(false)
                .qrCodeValue(UUID.randomUUID().toString())
                .status(MemberStatus.ACTIVE)
                .registeredBy(principal.getMemberId())
                .build();

        return MemberResponse.from(memberRepository.save(member));
    }

    @Transactional(readOnly = true)
    public Page<MemberResponse> getAllMembers(MemberPrincipal principal, String search, Pageable pageable) {
        // Everyone may browse the church directory (2026-07-12). Privileged roles
        // get full records incl. deactivated members; regular members see ACTIVE
        // members with NAME + PHONE only (fromDirectory — no email/DOB/role/QR).
        // Optional ?search= filters case-insensitively by name / email / phone.
        boolean isPrivileged = RoleChecker.isPastorOrElder(principal)
                || principal.getRole() == Role.MANAGER;
        boolean hasSearch = search != null && !search.trim().isEmpty();
        String term = hasSearch ? search.trim() : null;
        UUID churchId = principal.getChurchId();

        if (isPrivileged) {
            Page<Member> page = hasSearch
                    ? memberRepository.searchByChurchId(churchId, term, pageable)
                    : memberRepository.findByChurchId(churchId, pageable);
            return page.map(MemberResponse::from);
        }
        Page<Member> page = hasSearch
                ? memberRepository.searchByChurchIdAndStatus(churchId, MemberStatus.ACTIVE, term, pageable)
                : memberRepository.findByChurchIdAndStatus(churchId, MemberStatus.ACTIVE, pageable);
        return page.map(MemberResponse::fromDirectory);
    }

    @Transactional(readOnly = true)
    public MemberResponse getMember(UUID memberId, MemberPrincipal principal) {
        boolean isPrivileged = RoleChecker.isPastorOrElder(principal) || principal.getRole() == Role.MANAGER;
        boolean isOwnProfile = principal.getMemberId().equals(memberId);

        Member member = memberRepository.findByChurchIdAndId(principal.getChurchId(), memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        // Regular members may view others too — but only the directory view (name + phone)
        if (!isPrivileged && !isOwnProfile) {
            if (member.getStatus() != MemberStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found");
            }
            return MemberResponse.fromDirectory(member);
        }

        return MemberResponse.from(member);
    }

    public MemberResponse updateMember(UUID memberId, UpdateMemberRequest request, MemberPrincipal principal) {
        boolean isPrivileged = RoleChecker.isPastorOrElder(principal) || principal.getRole() == Role.MANAGER;
        boolean isOwnProfile = principal.getMemberId().equals(memberId);

        if (!isPrivileged && !isOwnProfile) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Member member = memberRepository.findByChurchIdAndId(principal.getChurchId(), memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        if (request.getFullName() != null) member.setFullName(request.getFullName());
        if (request.getPhone() != null) member.setPhone(request.getPhone());
        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(member.getEmail())) {
            // M1: reject if the new email already belongs to a different member in this church.
            memberRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
                if (!existing.getId().equals(memberId)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
                }
            });
            member.setEmail(request.getEmail());
            member.setEmailVerified(false);
            member.setTokenVersion(member.getTokenVersion() + 1);
            // Revoke all refresh tokens so a stolen token cannot survive the email change
            refreshTokenRepository.revokeAllForMember(memberId);
        }
        if (request.getDateOfBirth() != null) member.setDateOfBirth(request.getDateOfBirth());
        if (request.getCategory() != null) member.setCategory(request.getCategory());
        if (request.getAddress() != null) member.setAddress(request.getAddress());
        if (request.getBaptismDate() != null) member.setBaptismDate(request.getBaptismDate());
        if (request.getMembershipDate() != null) member.setMembershipDate(request.getMembershipDate());

        return MemberResponse.from(memberRepository.save(member));
    }

    @Transactional(readOnly = true)
    public String getQrCode(UUID memberId, MemberPrincipal principal) {
        boolean isPrivileged = RoleChecker.isPastorOrElder(principal) || principal.getRole() == Role.MANAGER;
        boolean isOwnQr = principal.getMemberId().equals(memberId);

        if (!isPrivileged && !isOwnQr) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Member member = memberRepository.findByChurchIdAndId(principal.getChurchId(), memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        return member.getQrCodeValue();
    }

    public void deactivateMember(UUID memberId, MemberPrincipal principal) {
        RoleChecker.requirePastorOrElder(principal);

        Member member = memberRepository.findByChurchIdAndId(principal.getChurchId(), memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        if (member.getStatus() == MemberStatus.DEACTIVATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member is already deactivated");
        }

        if (member.getRole() == Role.PASTOR) {
            long elderCount = memberRepository.countByChurchIdAndRole(principal.getChurchId(), Role.ELDER);
            if (elderCount == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot deactivate the only Pastor when no Elders exist — the church would have no way to appoint a new Pastor");
            }
        }

        member.setStatus(MemberStatus.DEACTIVATED);
        member.setDeactivatedBy(principal.getMemberId());
        member.setDeactivatedAt(LocalDateTime.now());
        memberRepository.save(member);
    }

    /**
     * A member removes THEMSELVES from the church (self-service, any role).
     * Deactivates the account, kills every session (tokenVersion bump + refresh
     * revocation), and clears the push token so the device stops receiving pushes.
     * Leadership can still see the record and reactivate if the person returns.
     */
    public void leaveChurch(MemberPrincipal principal) {
        Member member = memberRepository.findByChurchIdAndId(principal.getChurchId(), principal.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        // Same safeguard as deactivateMember: the only Pastor cannot walk away
        // when no Elder exists — nobody could ever appoint a replacement.
        if (member.getRole() == Role.PASTOR) {
            long elderCount = memberRepository.countByChurchIdAndRole(principal.getChurchId(), Role.ELDER);
            if (elderCount == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "You are the only Pastor and there are no Elders. Appoint an Elder before leaving so the church can continue.");
            }
        }

        member.setStatus(MemberStatus.DEACTIVATED);
        member.setDeactivatedBy(principal.getMemberId()); // self
        member.setDeactivatedAt(LocalDateTime.now());
        member.setFcmToken(null);
        member.setTokenVersion(member.getTokenVersion() + 1);
        memberRepository.save(member);
        refreshTokenRepository.revokeAllForMember(principal.getMemberId());

        auditLog.memberLeftChurch(principal.getMemberId(), principal.getChurchId());
    }

    public void reactivateMember(UUID memberId, MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);

        Member member = memberRepository.findByChurchIdAndId(principal.getChurchId(), memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        if (member.getStatus() == MemberStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member is already active");
        }

        member.setStatus(MemberStatus.ACTIVE);
        member.setDeactivatedBy(null);
        member.setDeactivatedAt(null);
        memberRepository.save(member);
    }

    public MemberResponse assignRole(UUID memberId, AssignRoleRequest request, MemberPrincipal principal) {
        Role callerRole = principal.getRole();
        Role newRole = request.getRole();

        if (newRole == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }

        Member target = memberRepository.findByChurchIdAndId(principal.getChurchId(), memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        Role currentRole = target.getRole();

        // Managers can only demote a Pastor or Elder back to Member
        if (callerRole == Role.MANAGER) {
            if (newRole != Role.MEMBER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Managers can only demote Pastors or Elders to Member");
            }
            if (currentRole != Role.PASTOR && currentRole != Role.ELDER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Managers can only demote Pastors or Elders");
            }
            target.setRole(Role.MEMBER);
            return MemberResponse.from(memberRepository.save(target));
        }

        if (callerRole != Role.PASTOR && callerRole != Role.ELDER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a Pastor, Elder, or Manager can assign roles");
        }

        // Pastor rules
        if (callerRole == Role.PASTOR) {
            if (currentRole == Role.ELDER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Pastor cannot demote an Elder");
            }
            if (newRole == Role.PASTOR) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Pastor cannot appoint another Pastor — only an Elder can do this");
            }
        }

        // Elder rules
        if (callerRole == Role.ELDER) {
            if (newRole == Role.MANAGER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only the Pastor can assign Managers");
            }
            if (newRole == Role.ELDER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only the Pastor can appoint Elders");
            }
            if (currentRole == Role.ELDER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Elders cannot demote other Elders");
            }
        }

        if (newRole == Role.ELDER) {
            long elderCount = memberRepository.countByChurchIdAndRole(principal.getChurchId(), Role.ELDER);
            if (elderCount >= MAX_ELDERS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Maximum " + MAX_ELDERS + " elders reached");
            }
        }

        if (newRole == Role.MANAGER) {
            long managerCount = memberRepository.countByChurchIdAndRole(principal.getChurchId(), Role.MANAGER);
            if (managerCount >= MAX_MANAGERS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Maximum " + MAX_MANAGERS + " managers reached");
            }
        }

        if (newRole == Role.PASTOR) {
            memberRepository.findByChurchIdAndRole(principal.getChurchId(), Role.PASTOR)
                    .forEach(existingPastor -> {
                        existingPastor.setRole(Role.MEMBER);
                        memberRepository.save(existingPastor);
                    });
        }

        target.setRole(newRole);
        return MemberResponse.from(memberRepository.save(target));
    }

    public String uploadMemberPhoto(UUID memberId, MultipartFile file, MemberPrincipal principal) {
        if (!principal.getMemberId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update your own profile picture");
        }

        Member member = memberRepository.findByChurchIdAndId(principal.getChurchId(), memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        if (member.getPhotoUrl() != null) {
            storageService.deleteFile(member.getPhotoUrl());
        }

        String url = storageService.uploadImage(file, "members/" + memberId);
        member.setPhotoUrl(url);
        memberRepository.save(member);

        return url;
    }

    public MessageResponse updatePhone(UpdatePhoneRequest request, MemberPrincipal principal, String ip) {
        if (rateLimiterService.checkUpdatePhone(principal.getMemberId().toString())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many phone update attempts. Try again later.");
        }

        Member member = memberRepository.findByChurchIdAndId(principal.getChurchId(), principal.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        String newPhone = request.getPhoneNumber();
        String oldPhone = member.getPhoneNumber();
        boolean isNewNumber = !newPhone.equals(oldPhone);

        if (isNewNumber) {
            memberRepository.findByPhoneNumber(newPhone).ifPresent(existing -> {
                if (!existing.getId().equals(principal.getMemberId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already in use");
                }
            });
            member.setPhoneNumber(newPhone);
            member.setPhoneVerified(false);
        } else if (Boolean.TRUE.equals(member.getPhoneVerified())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is already verified");
        }

        String rawCode = generateSixDigitCode();
        member.setPhoneVerificationCodeHash(sha256(rawCode));
        member.setPhoneVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
        member.setPhoneVerificationAttempts(0);
        member.setLastPhoneVerificationAttemptAt(null);
        memberRepository.save(member);

        eventPublisher.publishEvent(new SmsVerificationEvent(this, member.getPhoneNumber(), member.getFullName(), rawCode));

        if (isNewNumber) {
            if (oldPhone == null) {
                auditLog.phoneNumberAdded(member.getId(), newPhone, ip);
            } else {
                auditLog.phoneNumberChanged(member.getId(), newPhone, ip);
            }
        }

        return new MessageResponse("Verification code sent to your phone. Please verify the number.");
    }

    public MessageResponse registerFcmToken(String token, MemberPrincipal principal) {
        Member member = memberRepository.findByChurchIdAndId(principal.getChurchId(), principal.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        member.setFcmToken(token);
        memberRepository.save(member);
        return new MessageResponse("FCM token registered");
    }

    public MessageResponse clearFcmToken(MemberPrincipal principal) {
        memberRepository.findByChurchIdAndId(principal.getChurchId(), principal.getMemberId())
                .ifPresent(member -> {
                    member.setFcmToken(null);
                    memberRepository.save(member);
                });
        return new MessageResponse("FCM token cleared");
    }

    private String generateSixDigitCode() {
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

    private String sha256(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes());
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

}
