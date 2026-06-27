package com.example.demo.repository;

import com.example.demo.model.ProjectUpdate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectUpdateRepository extends JpaRepository<ProjectUpdate, UUID> {

    Page<ProjectUpdate> findByProjectIdOrderByPostedAtDesc(UUID projectId, Pageable pageable);

    List<ProjectUpdate> findTop5ByChurchIdOrderByPostedAtDesc(UUID churchId);

    @Modifying
    @Query("DELETE FROM ProjectUpdate pu WHERE pu.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}