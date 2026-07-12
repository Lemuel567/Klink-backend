package com.example.demo.repository;

import com.example.demo.model.OnlinePaymentStatus;
import com.example.demo.model.OnlinePaymentType;
import com.example.demo.model.PaystackTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaystackTransactionRepository extends JpaRepository<PaystackTransaction, UUID> {

    Optional<PaystackTransaction> findByPaystackReference(String reference);

    // SELECT FOR UPDATE — serialises concurrent verify/webhook completion of the same
    // transaction so the ledger record can never be materialised twice.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PaystackTransaction t WHERE t.paystackReference = :reference")
    Optional<PaystackTransaction> findByPaystackReferenceForUpdate(@Param("reference") String reference);

    Page<PaystackTransaction> findByChurchIdAndMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID churchId, UUID memberId, Pageable pageable);

    Page<PaystackTransaction> findByChurchIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID churchId, Pageable pageable);

    Page<PaystackTransaction> findByChurchIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID churchId, OnlinePaymentStatus status, Pageable pageable);

    List<PaystackTransaction> findTop10ByChurchIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID churchId, OnlinePaymentStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaystackTransaction t "
            + "WHERE t.churchId = :churchId AND t.status = :status "
            + "AND t.createdAt BETWEEN :from AND :to AND t.deletedAt IS NULL")
    BigDecimal sumAmountByChurchIdAndStatusBetween(@Param("churchId") UUID churchId,
                                                   @Param("status") OnlinePaymentStatus status,
                                                   @Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to);

    long countByChurchIdAndStatusAndDeletedAtIsNull(UUID churchId, OnlinePaymentStatus status);

    long countByChurchIdAndStatusAndPaymentTypeAndDeletedAtIsNull(
            UUID churchId, OnlinePaymentStatus status, OnlinePaymentType paymentType);

    long countByChurchIdAndStatusAndChannelAndDeletedAtIsNull(
            UUID churchId, OnlinePaymentStatus status, String channel);

    @Modifying
    @Query("DELETE FROM PaystackTransaction t WHERE t.churchId = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
