package com.example.demo.repository;

import com.example.demo.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.memberId = :memberId")
    int revokeAllForMember(@Param("memberId") UUID memberId);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.familyId = :familyId")
    int revokeAllInFamily(@Param("familyId") UUID familyId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RefreshToken r WHERE r.revoked = true OR r.expiresAt < :cutoff")
    int deleteExpiredAndRevoked(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.memberId IN :memberIds")
    void deleteByMemberIdIn(@Param("memberIds") java.util.List<UUID> memberIds);
}
