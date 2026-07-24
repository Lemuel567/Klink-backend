package com.example.demo.repository;

import com.example.demo.model.LiveStream;
import com.example.demo.model.LiveStreamStatus;
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
public interface LiveStreamRepository extends JpaRepository<LiveStream, UUID> {

    Optional<LiveStream> findByChurchIdAndId(UUID churchId, UUID id);

    Page<LiveStream> findByChurchIdOrderByStartedAtDesc(UUID churchId, Pageable pageable);

    /** Any still-open stream for this church (used to auto-close a forgotten one). */
    List<LiveStream> findByChurchIdAndStatus(UUID churchId, LiveStreamStatus status);

    /**
     * The church's genuinely-current stream: still LIVE *and* started recently.
     * The recency bound is a stale-guard — a leader who forgets to tap "End"
     * must not leave a permanent "Live now" badge on every member's home screen.
     */
    Optional<LiveStream> findFirstByChurchIdAndStatusAndStartedAtAfterOrderByStartedAtDesc(
            UUID churchId, LiveStreamStatus status, LocalDateTime after);

    @Modifying
    @Query("DELETE FROM LiveStream l WHERE l.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
