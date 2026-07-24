package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A church's live broadcast. Klink stores ONLY metadata — the video itself is
 * ingested, transcoded and delivered entirely by YouTube, so no media ever
 * touches this database or the backend (see CLAUDE.md live-stream notes).
 */
@Entity
@Table(name = "live_streams", indexes = {
        @Index(name = "idx_live_streams_church_status", columnList = "church_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveStream {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "church_id", nullable = false)
    private Church church;

    @Column(name = "title", nullable = false)
    private String title;

    /** Which platform is carrying the broadcast (detected from the pasted link). */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private LiveStreamProvider provider;

    /**
     * Provider-specific handle, parsed from whatever URL the leader pasted:
     * YOUTUBE  -> the 11-char video id (the embed takes an id)
     * FACEBOOK -> the full canonical video URL (the embed takes a full href)
     */
    @Column(name = "source_ref", nullable = false, length = 600)
    private String sourceRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LiveStreamStatus status;

    /** Member who tapped "Go Live" (plain UUID, like Devotional.postedBy). */
    @Column(name = "started_by")
    private UUID startedBy;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
