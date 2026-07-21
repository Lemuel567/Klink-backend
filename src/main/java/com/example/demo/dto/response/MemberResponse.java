package com.example.demo.dto.response;

import com.example.demo.model.Category;
import com.example.demo.model.Member;
import com.example.demo.model.MemberStatus;
import com.example.demo.model.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class MemberResponse {

    private UUID id;
    private UUID churchId;
    private String fullName;
    private String email;
    private String phone;
    /** E.164 login number — full view only; never present in the directory view. */
    private String phoneNumber;
    private boolean emailVerified;
    private boolean phoneVerified;
    private Role role;
    private Category category;
    private boolean hasSmartphone;
    private String qrCodeValue;
    private LocalDate dateOfBirth;
    private MemberStatus status;
    private String photoUrl;
    private LocalDateTime createdAt;

    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .churchId(member.getChurch().getId())
                .fullName(member.getFullName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .phoneNumber(member.getPhoneNumber())
                .emailVerified(Boolean.TRUE.equals(member.getEmailVerified()))
                .phoneVerified(Boolean.TRUE.equals(member.getPhoneVerified()))
                .role(member.getRole())
                .category(member.getCategory())
                .hasSmartphone(member.isHasSmartphone())
                .qrCodeValue(member.getQrCodeValue())
                .dateOfBirth(member.getDateOfBirth())
                .status(member.getStatus())
                .photoUrl(member.getPhotoUrl())
                .createdAt(member.getCreatedAt())
                .build();
    }

    /**
     * Directory view for regular members: name and phone number ONLY.
     * No email, DOB, role, status, QR value, or any other PII.
     */
    public static MemberResponse fromDirectory(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .fullName(member.getFullName())
                .phone(member.getPhone())
                .photoUrl(member.getPhotoUrl())
                .build();
    }
}
