package com.example.demo.repository;

import com.example.demo.model.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, UUID> {

    List<AttendanceSession> findByProcessedFalseAndExpiresAtBefore(LocalDateTime now);

    @Modifying
    @Query("DELETE FROM AttendanceSession a WHERE a.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
