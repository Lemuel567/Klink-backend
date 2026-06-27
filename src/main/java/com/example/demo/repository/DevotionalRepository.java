package com.example.demo.repository;

import com.example.demo.model.Devotional;
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
public interface DevotionalRepository extends JpaRepository<Devotional, UUID> {

    java.util.Optional<Devotional> findByChurchIdAndId(UUID churchId, UUID id);

    List<Devotional> findByChurchIdOrderByDevotionalDateDesc(UUID churchId);

    Page<Devotional> findByChurchIdOrderByDevotionalDateDesc(UUID churchId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Devotional d WHERE d.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
