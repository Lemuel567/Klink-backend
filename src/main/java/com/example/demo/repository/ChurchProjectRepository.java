package com.example.demo.repository;

import com.example.demo.model.ChurchProject;
import com.example.demo.model.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChurchProjectRepository extends JpaRepository<ChurchProject, UUID> {

    Page<ChurchProject> findByChurchIdAndDeletedAtIsNull(UUID churchId, Pageable pageable);

    Page<ChurchProject> findByChurchIdAndStatusAndDeletedAtIsNull(UUID churchId, ProjectStatus status, Pageable pageable);

    Page<ChurchProject> findByChurchIdAndProjectTypeAndDeletedAtIsNull(UUID churchId, com.example.demo.model.ProjectType projectType, Pageable pageable);

    Page<ChurchProject> findByChurchIdAndIsPublicTrueAndDeletedAtIsNull(UUID churchId, Pageable pageable);

    Page<ChurchProject> findByChurchIdAndIsPublicTrueAndStatusAndDeletedAtIsNull(UUID churchId, ProjectStatus status, Pageable pageable);

    Optional<ChurchProject> findByChurchIdAndIdAndDeletedAtIsNull(UUID churchId, UUID id);

    @Query("SELECT COUNT(p) FROM ChurchProject p WHERE p.church.id = :churchId AND p.status = :status AND p.deletedAt IS NULL")
    long countByChurchIdAndStatus(@Param("churchId") UUID churchId, @Param("status") ProjectStatus status);

    @Query("SELECT COALESCE(SUM(p.targetAmount), 0) FROM ChurchProject p WHERE p.church.id = :churchId AND p.deletedAt IS NULL AND p.status NOT IN ('CANCELLED', 'COMPLETED')")
    BigDecimal sumTargetAmountActive(@Param("churchId") UUID churchId);

    @Query("SELECT COALESCE(SUM(p.amountRaised), 0) FROM ChurchProject p WHERE p.church.id = :churchId AND p.deletedAt IS NULL AND p.status NOT IN ('CANCELLED', 'COMPLETED')")
    BigDecimal sumAmountRaisedActive(@Param("churchId") UUID churchId);

    List<ChurchProject> findByChurchIdAndStatusAndExpectedEndDateBeforeAndDeletedAtIsNull(
            UUID churchId, ProjectStatus status, LocalDate date);

    @Modifying
    @Query("DELETE FROM ChurchProject cp WHERE cp.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}