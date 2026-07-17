package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Per-member read receipt for an announcement. One row = "this member has read
 * this announcement". Absence of a row means unread. Church-scoped for isolation
 * and cascade deletion.
 */
@Entity
@Table(
    name = "announcement_reads",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_announcement_read",
        columnNames = {"announcement_id", "member_id"}
    ),
    indexes = {
        @Index(name = "idx_announcement_reads_member", columnList = "church_id, member_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementRead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "church_id", nullable = false)
    private Church church;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @CreationTimestamp
    @Column(name = "read_at", updatable = false)
    private LocalDateTime readAt;
}
