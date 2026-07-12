package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prayer_requests", indexes = {
        @Index(name = "idx_prayer_church", columnList = "church_id"),
        @Index(name = "idx_prayer_member", columnList = "member_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrayerRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "church_id", nullable = false)
    private Church church;

    /** The member who submitted the request (author). */
    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private PrayerVisibility visibility = PrayerVisibility.PUBLIC;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PrayerStatus status = PrayerStatus.OPEN;

    /** Response written by a Pastor or Elder when marking the request answered. */
    @Column(name = "leader_response", columnDefinition = "TEXT")
    private String leaderResponse;

    @Column(name = "answered_by")
    private UUID answeredBy;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    // ── Timestamps / soft delete ────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
