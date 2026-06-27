package com.example.demo.repository;

import com.example.demo.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByChurchIdOrderByEventDateAsc(UUID churchId);

    Page<Event> findByChurchIdOrderByEventDateAsc(UUID churchId, Pageable pageable);

    java.util.Optional<Event> findByChurchIdAndId(UUID churchId, UUID id);

    // Events that haven't been reminded yet and are still upcoming (for reminder scheduler)
    List<Event> findByChurchIdAndReminderSentFalseAndEventDateAfter(UUID churchId, LocalDateTime now);

    // Cross-church: upcoming events in the next window that haven't been reminded (for scheduler)
    List<Event> findByReminderSentFalseAndEventDateBetween(LocalDateTime from, LocalDateTime to);

    @Modifying
    @Query("DELETE FROM Event e WHERE e.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
