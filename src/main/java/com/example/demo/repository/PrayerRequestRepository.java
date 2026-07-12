package com.example.demo.repository;

import com.example.demo.model.PrayerRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrayerRequestRepository extends JpaRepository<PrayerRequest, UUID> {

    Optional<PrayerRequest> findByChurchIdAndIdAndDeletedAtIsNull(UUID churchId, UUID id);

    /** Leaders (Pastor/Elder) see every non-deleted request in the church. */
    Page<PrayerRequest> findByChurchIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID churchId, Pageable pageable);

    /**
     * Regular members see all PUBLIC requests plus their own PRIVATE ones.
     * Filtered by church_id and excludes soft-deleted rows.
     */
    @Query("SELECT p FROM PrayerRequest p WHERE p.church.id = :churchId AND p.deletedAt IS NULL "
            + "AND (p.visibility = com.example.demo.model.PrayerVisibility.PUBLIC OR p.memberId = :memberId) "
            + "ORDER BY p.createdAt DESC")
    Page<PrayerRequest> findVisibleForMember(@Param("churchId") UUID churchId,
                                             @Param("memberId") UUID memberId,
                                             Pageable pageable);

    @Modifying
    @Query("DELETE FROM PrayerRequest p WHERE p.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
