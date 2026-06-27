package com.example.demo.repository;

import com.example.demo.model.Pledge;
import com.example.demo.model.PledgeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PledgeRepository extends JpaRepository<Pledge, UUID> {

    // Financial Secretary: view all pledges for the church
    List<Pledge> findByChurchId(UUID churchId);

    Page<Pledge> findByChurchId(UUID churchId, Pageable pageable);

    // Financial Secretary: filter all church pledges by status (e.g. all UNPAID)
    List<Pledge> findByChurchIdAndStatus(UUID churchId, PledgeStatus status);

    // Financial Secretary: look up a specific pledge to mark it paid
    Optional<Pledge> findByChurchIdAndId(UUID churchId, UUID id);

    // Member: view all their own pledges
    List<Pledge> findByChurchIdAndMemberId(UUID churchId, UUID memberId);

    Page<Pledge> findByChurchIdAndMemberId(UUID churchId, UUID memberId, Pageable pageable);

    // Member: view their own pledges filtered by status (e.g. only UNPAID)
    List<Pledge> findByChurchIdAndMemberIdAndStatus(UUID churchId, UUID memberId, PledgeStatus status);

    List<Pledge> findByStatus(PledgeStatus status);

    List<Pledge> findByStatusIn(List<PledgeStatus> statuses);

    // Paginated version used by PledgeReminderScheduler — avoids loading all pledges into memory at once
    @EntityGraph(attributePaths = "member")
    @Query("SELECT p FROM Pledge p WHERE p.status IN :statuses")
    Page<Pledge> findByStatusInPaged(@Param("statuses") List<PledgeStatus> statuses, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Pledge p WHERE p.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
