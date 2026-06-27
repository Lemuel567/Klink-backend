package com.example.demo.repository;

import com.example.demo.model.PledgePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PledgePaymentRepository extends JpaRepository<PledgePayment, UUID> {

    List<PledgePayment> findByChurchIdAndPledgeId(UUID churchId, UUID pledgeId);

    @Modifying
    @Query("DELETE FROM PledgePayment pp WHERE pp.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
