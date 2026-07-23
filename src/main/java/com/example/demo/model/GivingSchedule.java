package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A member's standing intention to give a fixed amount each month. On the chosen
 * day the app reminds them (push/SMS) with a one-tap link to pay — reliable for
 * Mobile Money, which cannot be silently auto-debited.
 */
@Entity
@Table(
    name = "giving_schedules",
    indexes = @Index(name = "idx_giving_sched_active_day", columnList = "active, day_of_month")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GivingSchedule {

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

    // length matches V3__giving_schedules.sql exactly — required for a future
    // switch to HIBERNATE_DDL_AUTO=validate
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 40)
    private OnlinePaymentType paymentType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** 1–28 (capped so it exists in every month). */
    @Column(name = "day_of_month", nullable = false)
    private int dayOfMonth;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** yyyy-MM of the last month a reminder fired — prevents duplicate reminders. */
    @Column(name = "last_run_month", length = 7)
    private String lastRunMonth;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
