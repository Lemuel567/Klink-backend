package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "members",
    indexes = {
        @Index(name = "idx_members_church_id", columnList = "church_id"),
        @Index(name = "idx_members_status", columnList = "status"),
        @Index(name = "idx_members_church_status", columnList = "church_id, status"),
        @Index(name = "idx_members_date_of_birth", columnList = "date_of_birth")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "church_id", nullable = false)
    private Church church;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone")
    private String phone;

    // Unique — email is a login identifier; without this constraint a race between
    // the existence check and the insert can create duplicates and break findByEmail
    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private Category category;

    @Column(name = "has_smartphone", nullable = false)
    private boolean hasSmartphone;

    @Column(name = "qr_code_value", unique = true)
    private String qrCodeValue;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "address")
    private String address;

    @Column(name = "baptism_date")
    private LocalDate baptismDate;

    @Column(name = "membership_date")
    private LocalDate membershipDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MemberStatus status;

    @Column(name = "deactivated_by")
    private UUID deactivatedBy;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Column(name = "registered_by")
    private UUID registeredBy;

    @Column(name = "auth_user_id")
    private UUID authUserId;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "photo_url")
    private String photoUrl;

    @Builder.Default
    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Builder.Default
    @Column(name = "phone_verified")
    private Boolean phoneVerified = false;

    @Column(name = "phone_verification_code_hash")
    private String phoneVerificationCodeHash;

    @Column(name = "phone_verification_code_expires_at")
    private LocalDateTime phoneVerificationCodeExpiresAt;

    @Builder.Default
    @Column(name = "phone_verification_attempts", nullable = false, columnDefinition = "integer not null default 0")
    private int phoneVerificationAttempts = 0;

    @Column(name = "last_phone_verification_attempt_at")
    private LocalDateTime lastPhoneVerificationAttemptAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "failed_login_attempts", nullable = false, columnDefinition = "integer not null default 0")
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "last_failed_at")
    private LocalDateTime lastFailedAt;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "token_version", nullable = false, columnDefinition = "integer not null default 0")
    @Builder.Default
    private int tokenVersion = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
