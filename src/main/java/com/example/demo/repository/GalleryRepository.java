package com.example.demo.repository;

import com.example.demo.model.Gallery;
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
public interface GalleryRepository extends JpaRepository<Gallery, UUID> {

    List<Gallery> findByChurchIdOrderByUploadedAtDesc(UUID churchId);

    Page<Gallery> findByChurchIdOrderByUploadedAtDesc(UUID churchId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Gallery g WHERE g.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
