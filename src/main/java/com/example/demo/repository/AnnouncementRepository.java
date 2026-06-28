package com.example.demo.repository;

import com.example.demo.model.Announcement;
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
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    java.util.Optional<Announcement> findByChurchIdAndId(UUID churchId, UUID id);

    List<Announcement> findByChurchIdOrderByCreatedAtDesc(UUID churchId);

    Page<Announcement> findByChurchIdOrderByCreatedAtDesc(UUID churchId, Pageable pageable);

    List<Announcement> findTop200ByChurchIdOrderByCreatedAtDesc(UUID churchId);

    @Modifying
    @Query("DELETE FROM Announcement a WHERE a.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
