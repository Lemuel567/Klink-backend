package com.example.demo.repository;

import com.example.demo.model.Sermon;
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
public interface SermonRepository extends JpaRepository<Sermon, UUID> {

    java.util.Optional<Sermon> findByChurchIdAndId(UUID churchId, UUID id);

    List<Sermon> findByChurchIdOrderBySermonDateDesc(UUID churchId);

    Page<Sermon> findByChurchIdOrderBySermonDateDesc(UUID churchId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Sermon s WHERE s.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
