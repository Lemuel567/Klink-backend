package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "facilities",
    indexes = {
        @Index(name = "idx_facilities_church_id", columnList = "church_id"),
        @Index(name = "idx_facilities_type", columnList = "facility_type"),
        @Index(name = "idx_facilities_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_facilities_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "church_id", nullable = false)
    private Church church;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "facility_type", nullable = false, length = 30)
    private FacilityType facilityType;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "year_acquired")
    private Integer yearAcquired;

    @Column(name = "estimated_value", precision = 19, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "GHS";

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_status", nullable = false, length = 20)
    private FacilityCondition condition;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}