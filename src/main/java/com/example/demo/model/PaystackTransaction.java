package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An online payment made by a member through Paystack (card / mobile money).
 * On SUCCESS the transaction is materialised into the church ledger:
 * a Payment row (tithe/offering/welfare/special) or a ProjectContribution.
 * The name avoids colliding with the existing {@link Payment} ledger entity.
 */
@Entity
@Table(name = "paystack_transactions", indexes = {
        @Index(name = "idx_paytx_church", columnList = "church_id"),
        @Index(name = "idx_paytx_member", columnList = "member_id"),
        @Index(name = "idx_paytx_reference", columnList = "paystack_reference", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaystackTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "church_id", nullable = false)
    private UUID churchId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    /** Always stored in GHS; converted to pesewas only when calling Paystack. */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "currency", nullable = false)
    private String currency = "GHS";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private OnlinePaymentType paymentType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OnlinePaymentStatus status = OnlinePaymentStatus.PENDING;

    @Column(name = "paystack_reference", nullable = false, unique = true)
    private String paystackReference;

    @Column(name = "paystack_transaction_id")
    private String paystackTransactionId;

    @Column(name = "paystack_authorization_code")
    private String paystackAuthorizationCode;

    /** card | mobile_money | ussd | bank — as reported by Paystack. */
    @Column(name = "channel")
    private String channel;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "description")
    private String description;

    /** Set when paymentType is PROJECT_CONTRIBUTION. */
    @Column(name = "project_id")
    private UUID projectId;

    /** True once the ledger record (Payment / ProjectContribution) has been created. */
    @Builder.Default
    @Column(name = "is_recorded", nullable = false)
    private boolean isRecorded = false;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
