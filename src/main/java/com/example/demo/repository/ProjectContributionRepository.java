package com.example.demo.repository;

import com.example.demo.model.ProjectContribution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectContributionRepository extends JpaRepository<ProjectContribution, UUID> {

    Page<ProjectContribution> findByProjectIdAndChurchId(UUID projectId, UUID churchId, Pageable pageable);

    Page<ProjectContribution> findByMemberIdAndChurchId(UUID memberId, UUID churchId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM ProjectContribution c WHERE c.project.id = :projectId")
    BigDecimal sumAmountByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT COUNT(DISTINCT c.member.id) FROM ProjectContribution c WHERE c.project.id = :projectId")
    long countDistinctContributorsByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT AVG(c.amount) FROM ProjectContribution c WHERE c.project.id = :projectId")
    BigDecimal avgAmountByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT MAX(c.amount) FROM ProjectContribution c WHERE c.project.id = :projectId")
    BigDecimal maxAmountByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT MAX(c.contributionDate) FROM ProjectContribution c WHERE c.project.id = :projectId")
    LocalDate lastContributionDateByProjectId(@Param("projectId") UUID projectId);

    // Returns [memberId, totalAmount] pairs sorted by total descending — used for top contributors
    @Query("""
        SELECT c.member.id, SUM(c.amount) as total
        FROM ProjectContribution c
        WHERE c.church.id = :churchId AND c.contributionDate >= :since
        GROUP BY c.member.id
        ORDER BY total DESC
        """)
    List<Object[]> findTopContributorsSince(@Param("churchId") UUID churchId,
                                            @Param("since") LocalDate since,
                                            Pageable pageable);

    // Distinct member IDs who contributed to a project — used to notify on update posts
    @Query("SELECT DISTINCT c.member.id FROM ProjectContribution c WHERE c.project.id = :projectId")
    List<UUID> findDistinctMemberIdsByProjectId(@Param("projectId") UUID projectId);

    @Modifying
    @Query("DELETE FROM ProjectContribution pc WHERE pc.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}