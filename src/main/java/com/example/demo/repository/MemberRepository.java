package com.example.demo.repository;

import com.example.demo.model.*;
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
public interface MemberRepository extends JpaRepository<Member, UUID> {

    List<Member> findByChurchId(UUID churchId);

    Page<Member> findByChurchId(UUID churchId, Pageable pageable);

    // Directory search (2026-07-12): case-insensitive on fullName / email / phone,
    // always church-scoped. Two variants — leaders search everyone, regular
    // members search ACTIVE only (matches directory privacy in MemberService).
    @Query("""
        SELECT m FROM Member m
        WHERE m.church.id = :churchId
          AND (LOWER(m.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(m.email, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR COALESCE(m.phone, '') LIKE CONCAT('%', :search, '%'))
        """)
    Page<Member> searchByChurchId(@Param("churchId") UUID churchId,
                                  @Param("search") String search,
                                  Pageable pageable);

    @Query("""
        SELECT m FROM Member m
        WHERE m.church.id = :churchId
          AND m.status = :status
          AND (LOWER(m.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR COALESCE(m.phone, '') LIKE CONCAT('%', :search, '%'))
        """)
    Page<Member> searchByChurchIdAndStatus(@Param("churchId") UUID churchId,
                                           @Param("status") MemberStatus status,
                                           @Param("search") String search,
                                           Pageable pageable);

    Optional<Member> findByChurchIdAndId(UUID churchId, UUID id);

    Optional<Member> findByEmail(String email);

    Optional<Member> findByPhoneNumber(String phoneNumber);

    Optional<Member> findByQrCodeValue(String qrCodeValue);

    Optional<Member> findByAuthUserId(UUID authUserId);

    List<Member> findByChurchIdAndRole(UUID churchId, Role role);

    long countByChurchIdAndRole(UUID churchId, Role role);

    List<Member> findByChurchIdAndStatus(UUID churchId, MemberStatus status);

    Page<Member> findByChurchIdAndStatus(UUID churchId, MemberStatus status, Pageable pageable);

    // Matches month and day only — year is ignored so this works for annual birthday reminders
    @Query("""
        SELECT m FROM Member m
        WHERE m.church.id = :churchId
          AND m.status = :status
          AND m.dateOfBirth IS NOT NULL
          AND MONTH(m.dateOfBirth) = :month
          AND DAY(m.dateOfBirth) = :day
        """)
    List<Member> findBirthdayMembers(
            @Param("churchId") UUID churchId,
            @Param("month") int month,
            @Param("day") int day,
            @Param("status") MemberStatus status
    );

    // clearAutomatically = true flushes the EntityManager so the SELECT below sees the fresh DB value.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Member m SET m.failedLoginAttempts = m.failedLoginAttempts + 1, m.lastFailedAt = :lastFailedAt WHERE m.id = :id")
    void incrementFailedLoginAttempts(@Param("id") UUID id, @Param("lastFailedAt") java.time.LocalDateTime lastFailedAt);

    // Atomically increments phoneVerificationAttempts only when currently below the limit.
    // Returns 1 if the increment happened (attempt counted), 0 if already at or over the limit.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Member m SET m.phoneVerificationAttempts = m.phoneVerificationAttempts + 1, m.lastPhoneVerificationAttemptAt = :now WHERE m.id = :id AND m.phoneVerificationAttempts < :limit")
    int incrementPhoneVerificationAttemptsIfUnderLimit(@Param("id") UUID id, @Param("now") java.time.LocalDateTime now, @Param("limit") int limit);

    // Separate SELECT after increment gives the real post-increment count from the DB — no stale value.
    @Query("SELECT m.failedLoginAttempts FROM Member m WHERE m.id = :id")
    int getFailedLoginAttempts(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Member m SET m.lockedUntil = :lockedUntil, m.failedLoginAttempts = 0 WHERE m.id = :id")
    void lockAccount(@Param("id") UUID id, @Param("lockedUntil") java.time.LocalDateTime lockedUntil);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Member m SET m.failedLoginAttempts = 0, m.lockedUntil = null, m.lastFailedAt = null WHERE m.id = :id")
    void resetLoginAttempts(@Param("id") UUID id);

    // Members of a church who have NOT recorded a welfare payment for the given month.
    // Uses NOT EXISTS instead of NOT IN to avoid the SQL NULL trap: NOT IN returns UNKNOWN
    // for all rows if the subquery contains any NULL, silently emptying the defaulters list.
    @Query("""
        SELECT m FROM Member m
        WHERE m.church.id = :churchId
          AND m.status = :status
          AND NOT EXISTS (
              SELECT 1 FROM Payment p
              WHERE p.church = m.church
                AND p.member = m
                AND p.paymentType = :paymentType
                AND p.paymentMonth = :paymentMonth
          )
        """)
    List<Member> findWelfareDefaulters(
            @Param("churchId") UUID churchId,
            @Param("paymentMonth") String paymentMonth,
            @Param("status") MemberStatus status,
            @Param("paymentType") PaymentType paymentType
    );

    @Query("""
        SELECT m FROM Member m
        WHERE m.church.id = :churchId
          AND m.status = :status
          AND NOT EXISTS (
              SELECT 1 FROM Payment p
              WHERE p.church = m.church
                AND p.member = m
                AND p.paymentType = :paymentType
                AND p.paymentMonth = :paymentMonth
          )
        """)
    Page<Member> findWelfareDefaultersPaged(
            @Param("churchId") UUID churchId,
            @Param("paymentMonth") String paymentMonth,
            @Param("status") MemberStatus status,
            @Param("paymentType") PaymentType paymentType,
            Pageable pageable
    );

    // Members not checked in for a specific service — used by AttendanceScheduler to bulk-insert ABSENT records.
    // NOT EXISTS instead of NOT IN: if any attendance row had a NULL member_id, NOT IN would return no rows at all.
    @Query("""
        SELECT m FROM Member m
        WHERE m.church.id = :churchId
          AND m.status = :status
          AND NOT EXISTS (
              SELECT 1 FROM Attendance a
              WHERE a.member.id = m.id
                AND a.serviceDate = :serviceDate
                AND a.serviceName = :serviceName
          )
        """)
    List<Member> findMembersNotCheckedIn(
            @Param("churchId") UUID churchId,
            @Param("status") MemberStatus status,
            @Param("serviceDate") java.time.LocalDate serviceDate,
            @Param("serviceName") String serviceName
    );

    // Birthday members across all churches (used by BirthdayScheduler — tiny result set on any given day)
    @Query("""
        SELECT m FROM Member m
        WHERE m.status = :status
          AND m.dateOfBirth IS NOT NULL
          AND MONTH(m.dateOfBirth) = :month
          AND DAY(m.dateOfBirth) = :day
        """)
    List<Member> findAllBirthdayMembersGlobal(
            @Param("month") int month,
            @Param("day") int day,
            @Param("status") MemberStatus status
    );

    // Eagerly fetches church to allow deleted-church checks in JwtFilter (outside a transaction).
    @Query("SELECT m FROM Member m JOIN FETCH m.church WHERE m.id = :id")
    Optional<Member> findByIdWithChurch(@Param("id") UUID id);

    List<Member> findByChurchIdAndRoleIn(UUID churchId, List<Role> roles);

    List<Member> findByChurchIdAndIdIn(UUID churchId, List<UUID> ids);

    @Modifying
    @Query("DELETE FROM Member m WHERE m.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
