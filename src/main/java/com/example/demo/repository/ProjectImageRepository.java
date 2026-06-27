package com.example.demo.repository;

import com.example.demo.model.ProjectImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectImageRepository extends JpaRepository<ProjectImage, UUID> {

    List<ProjectImage> findByProjectIdOrderBySortOrderAscUploadedAtAsc(UUID projectId);

    List<ProjectImage> findByProjectIdAndPhaseOrderBySortOrderAscUploadedAtAsc(UUID projectId, String phase);

    Optional<ProjectImage> findByIdAndProjectId(UUID id, UUID projectId);

    @Modifying
    @Query("UPDATE ProjectImage pi SET pi.isPrimary = false WHERE pi.project.id = :projectId")
    void clearPrimaryForProject(@Param("projectId") UUID projectId);

    List<ProjectImage> findByChurchId(UUID churchId);

    @Modifying
    @Query("DELETE FROM ProjectImage pi WHERE pi.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}