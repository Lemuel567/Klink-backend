package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sermons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sermon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "church_id", nullable = false)
    private Church church;

    @Column(name = "preacher", nullable = false)
    private String preacher;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "memory_verse")
    private String memoryVerse;

    @Column(name = "scripture")
    private String scripture;

    @Column(name = "sermon_date", nullable = false)
    private LocalDate sermonDate;

    @Column(name = "audio_url")
    private String audioUrl;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "posted_by")
    private UUID postedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
