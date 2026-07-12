package com.example.demo.scheduler;

import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;

    // Every Sunday at 03:00 — removes revoked/expired refresh tokens and used/expired verification codes
    @Scheduled(cron = "0 0 3 * * SUN")
    @Transactional
    public void purgeExpiredAndRevokedTokens() {
        int refreshDeleted = refreshTokenRepository.deleteExpiredAndRevoked(LocalDateTime.now());
        int verificationDeleted = verificationTokenRepository.deleteUsedAndExpired(LocalDateTime.now());
        log.info("Token cleanup: purged {} refresh token(s), {} verification token(s)",
                refreshDeleted, verificationDeleted);
    }
}
