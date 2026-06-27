package com.example.demo.repository;

import com.example.demo.model.StorePayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StorePaymentRepository extends JpaRepository<StorePayment, UUID> {

    List<StorePayment> findByChurchId(UUID churchId);

    Page<StorePayment> findByChurchId(UUID churchId, Pageable pageable);

    List<StorePayment> findByChurchIdAndMemberId(UUID churchId, UUID memberId);

    Page<StorePayment> findByChurchIdAndMemberId(UUID churchId, UUID memberId, Pageable pageable);

    List<StorePayment> findByChurchIdAndItemId(UUID churchId, UUID itemId);

    Optional<StorePayment> findByChurchIdAndId(UUID churchId, UUID id);

    @Modifying
    @Query("DELETE FROM StorePayment sp WHERE sp.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
