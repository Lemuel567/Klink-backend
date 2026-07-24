package com.example.demo.repository;

import com.example.demo.model.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    List<Attendance> findByChurchId(UUID churchId);

    Page<Attendance> findByChurchId(UUID churchId, Pageable pageable);

    List<Attendance> findByChurchIdAndMemberId(UUID churchId, UUID memberId);

    Page<Attendance> findByChurchIdAndMemberId(UUID churchId, UUID memberId, Pageable pageable);

    boolean existsByMemberIdAndServiceDateAndServiceName(UUID memberId, LocalDate serviceDate, String serviceName);

    // Analytics: how many were present in a month.
    long countByChurchIdAndStatusAndServiceDateBetween(
            UUID churchId, com.example.demo.model.AttendanceStatus status,
            LocalDate from, LocalDate to);

    // Personal journey: how many services this member has attended (PRESENT).
    long countByChurchIdAndMemberIdAndStatus(
            UUID churchId, UUID memberId, com.example.demo.model.AttendanceStatus status);

    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
