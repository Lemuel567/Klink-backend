package com.example.demo.repository;

import com.example.demo.model.VerificationToken;
import com.example.demo.model.VerificationTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(
            String email, VerificationTokenType type);

    @Modifying
    @Query("UPDATE VerificationToken vt SET vt.used = true WHERE vt.email = :email AND vt.type = :type AND vt.used = false")
    void markAllUnusedAsUsed(@Param("email") String email, @Param("type") VerificationTokenType type);

    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.email IN :emails")
    void deleteByEmailIn(@Param("emails") java.util.List<String> emails);

    // Housekeeping: used codes and expired codes serve no purpose — purge weekly
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM VerificationToken vt WHERE vt.used = true OR vt.expiresAt < :cutoff")
    int deleteUsedAndExpired(@Param("cutoff") java.time.LocalDateTime cutoff);
}
