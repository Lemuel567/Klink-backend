package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "church_id", nullable = false)
    private Church church;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "flyer_url")
    private String flyerUrl;

    @Column(name = "posted_by")
    private UUID postedBy;

    // ── Targeting ────────────────────────────────────────────────────────────

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private AnnouncementTargetType targetType = AnnouncementTargetType.ALL;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_roles", columnDefinition = "jsonb")
    private List<Role> targetRoles;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_group_ids", columnDefinition = "jsonb")
    private List<UUID> targetGroupIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_member_ids", columnDefinition = "jsonb")
    private List<UUID> targetMemberIds;

    @Builder.Default
    @Column(name = "is_targeted")
    private Boolean isTargeted = false;

    @Builder.Default
    @Column(name = "recipient_count")
    private Integer recipientCount = 0;

    // ── Timestamps ────────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
