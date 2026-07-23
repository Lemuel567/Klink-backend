package com.example.demo.repository;

import com.example.demo.model.Payment;
import com.example.demo.model.PaymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // Per-type collections over a date range, split by the `online` flag (set
    // only by the Paystack materialiser — a FinSec-recorded payment stays
    // "manual" even when it carries a MoMo receipt number). Church money only —
    // group dues (group != null) are excluded so they never mix into a church total.
    @Query("""
        SELECT p.paymentType AS type,
               COALESCE(SUM(CASE WHEN p.online = false THEN p.amount ELSE 0 END), 0) AS manualTotal,
               COALESCE(SUM(CASE WHEN p.online = true THEN p.amount ELSE 0 END), 0) AS onlineTotal
        FROM Payment p
        WHERE p.church.id = :churchId
          AND p.status = com.example.demo.model.PaymentStatus.CONFIRMED
          AND p.group IS NULL
          AND p.paymentDate BETWEEN :from AND :to
        GROUP BY p.paymentType
        """)
    List<CollectionTotal> summariseCollections(@Param("churchId") UUID churchId,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    // Only a MANUAL lump-sum offering (member is null) blocks a second manual
    // offering for the same service date — members' own online offerings that
    // day must NOT block the secretary recording the counted cash.
    boolean existsByChurchIdAndPaymentTypeAndPaymentDateAndMemberIsNull(
            UUID churchId, PaymentType paymentType, LocalDate paymentDate);

    List<Payment> findByChurchId(UUID churchId);

    List<Payment> findByChurchIdAndMemberId(UUID churchId, UUID memberId);

    Page<Payment> findByChurchIdAndMemberId(UUID churchId, UUID memberId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.church.id = :churchId AND p.member.id = :memberId AND p.paymentType IN :types")
    Page<Payment> findByChurchIdAndMemberIdAndPaymentTypeIn(
            @Param("churchId") UUID churchId,
            @Param("memberId") UUID memberId,
            @Param("types") List<PaymentType> types,
            Pageable pageable);

    List<Payment> findByChurchIdAndPaymentType(UUID churchId, PaymentType paymentType);

    List<Payment> findByChurchIdAndMemberIdAndPaymentType(UUID churchId, UUID memberId, PaymentType paymentType);

    List<Payment> findByChurchIdAndMemberIdAndPaymentTypeAndPaymentMonth(
            UUID churchId, UUID memberId, PaymentType paymentType, String paymentMonth);

    List<Payment> findByGroupId(UUID groupId);

    List<Payment> findByGroupIdAndPaymentMonth(UUID groupId, String paymentMonth);

    // Group money — always scoped to a single group_id and never mixed into any
    // church-wide total (church finance queries only sum OFFERING/TITHE/WELFARE).
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
        WHERE p.group.id = :groupId AND p.status = com.example.demo.model.PaymentStatus.CONFIRMED
        """)
    java.math.BigDecimal sumConfirmedByGroupId(@Param("groupId") UUID groupId);

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
        WHERE p.group.id = :groupId AND p.paymentMonth = :paymentMonth
          AND p.status = com.example.demo.model.PaymentStatus.CONFIRMED
        """)
    java.math.BigDecimal sumConfirmedByGroupIdAndMonth(
            @Param("groupId") UUID groupId, @Param("paymentMonth") String paymentMonth);

    long countByGroupIdAndPaymentMonthAndStatus(
            UUID groupId, String paymentMonth, com.example.demo.model.PaymentStatus status);

    List<Payment> findTop10ByGroupIdOrderByCreatedAtDesc(UUID groupId);

    boolean existsByChurchIdAndPaymentTypeAndPaymentDate(UUID churchId, PaymentType paymentType, java.time.LocalDate paymentDate);

    @Query("SELECT p FROM Payment p WHERE p.group.id = :groupId AND p.member.id = :memberId AND p.paymentMonth = :paymentMonth")
    List<Payment> findByGroupIdAndMemberIdAndPaymentMonth(
            @Param("groupId") UUID groupId,
            @Param("memberId") UUID memberId,
            @Param("paymentMonth") String paymentMonth);

    @Modifying
    @Query("DELETE FROM Payment p WHERE p.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
