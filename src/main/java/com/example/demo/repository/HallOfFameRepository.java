package com.example.demo.repository;

import com.example.demo.model.HallOfFame;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HallOfFameRepository extends JpaRepository<HallOfFame, UUID> {

    List<HallOfFame> findByChurchIdOrderByCreatedAtDesc(UUID churchId);

    Page<HallOfFame> findByChurchIdOrderByCreatedAtDesc(UUID churchId, Pageable pageable);

    Optional<HallOfFame> findByChurchIdAndId(UUID churchId, UUID id);

    @Modifying
    @Query("DELETE FROM HallOfFame h WHERE h.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
