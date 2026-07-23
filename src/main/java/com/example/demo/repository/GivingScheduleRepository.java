package com.example.demo.repository;

import com.example.demo.model.GivingSchedule;
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
public interface GivingScheduleRepository extends JpaRepository<GivingSchedule, UUID> {

    List<GivingSchedule> findByChurchIdAndMemberIdOrderByCreatedAtDesc(UUID churchId, UUID memberId);

    Optional<GivingSchedule> findByChurchIdAndMemberIdAndId(UUID churchId, UUID memberId, UUID id);

    // Scheduler: active schedules due on a given day-of-month.
    Page<GivingSchedule> findByActiveTrueAndDayOfMonth(int dayOfMonth, Pageable pageable);

    @Modifying
    @Query("DELETE FROM GivingSchedule g WHERE g.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
