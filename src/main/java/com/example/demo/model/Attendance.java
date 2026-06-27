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
    name = "attendance",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_attendance_member_service",
        columnNames = {"member_id", "service_date", "service_name"}
    ),
    indexes = {
        @Index(name = "idx_attendance_church_id", columnList = "church_id"),
        @Index(name = "idx_attendance_member_id", columnList = "member_id"),
        @Index(name = "idx_attendance_service_date", columnList = "service_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "church_id", nullable = false)
    private Church church;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "time_of_scan")
    private LocalDateTime timeOfScan;

    @Column(name = "marked_by")
    private UUID markedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "method")
    private AttendanceMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AttendanceStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
