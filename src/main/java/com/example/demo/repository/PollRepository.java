package com.example.demo.repository;

import com.example.demo.model.Poll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PollRepository extends JpaRepository<Poll, UUID> {

    List<Poll> findByChurchIdOrderByCreatedAtDesc(UUID churchId);

    Page<Poll> findByChurchIdOrderByCreatedAtDesc(UUID churchId, Pageable pageable);

    List<Poll> findByChurchIdAndClosesAtAfter(UUID churchId, LocalDateTime now);

    Optional<Poll> findByChurchIdAndId(UUID churchId, UUID id);

    @Modifying
    @Query("DELETE FROM Poll p WHERE p.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
