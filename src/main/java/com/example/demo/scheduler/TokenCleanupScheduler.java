package com.example.demo.scheduler;

import com.example.demo.repository.RefreshTokenRepository;
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

    // Every Sunday at 03:00 — removes revoked tokens and tokens past their expiry
    @Scheduled(cron = "0 0 3 * * SUN")
    @Transactional
    public void purgeExpiredAndRevokedTokens() {
        int deleted = refreshTokenRepository.deleteExpiredAndRevoked(LocalDateTime.now());
        log.info("Token cleanup: purged {} expired/revoked refresh token(s)", deleted);
    }
}
