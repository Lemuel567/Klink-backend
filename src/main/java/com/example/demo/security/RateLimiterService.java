package com.example.demo.security;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RateLimiterService {

    private final RateLimitProperties properties;

    // Login — two separate buckets: email+IP (per-account) and IP-only (per-network)
    private final Map<String, Deque<Long>> loginEmailIpAttempts = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> loginIpAttempts      = new ConcurrentHashMap<>();

    // Other auth endpoints — IP-only
    private final Map<String, Deque<Long>> registerAttempts    = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> forgotPwdAttempts   = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> resendVerifAttempts = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> refreshAttempts     = new ConcurrentHashMap<>();

    // Code submission endpoints — email-keyed (attacker targets a specific account)
    private final Map<String, Deque<Long>> verifyEmailAttempts  = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> resetPasswordAttempts = new ConcurrentHashMap<>();

    // Phone verification endpoints — phone-keyed or member-keyed
    private final Map<String, Deque<Long>> phoneVerifyAttempts  = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> phoneResendAttempts  = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> phoneUpdateAttempts  = new ConcurrentHashMap<>();

    // ── Login: check-only (no slot consumed on success) ─────────────────────

    /**
     * Returns true if either the per-account (email+IP) or per-IP login bucket is exhausted.
     * Does NOT consume a slot — call recordLoginFailure() only when the attempt actually fails.
     */
    public boolean isLoginLimitExhausted(String identifier, String ip) {
        RateLimitProperties.Login cfg = properties.getLogin();
        String accountKey = identifier.toLowerCase() + ":" + ip;
        return isAtLimit(loginEmailIpAttempts, accountKey, cfg.getMaxAttemptsPerAccount(), cfg.windowMillis())
                || isAtLimit(loginIpAttempts, ip, cfg.getMaxAttemptsPerIp(), cfg.windowMillis());
    }

    /**
     * Consumes one slot in both login buckets. Call this only when login fails
     * (wrong password or non-existent account). Never call on successful login.
     * identifier is the email address or phone number used to log in.
     */
    public void recordLoginFailure(String identifier, String ip) {
        RateLimitProperties.Login cfg = properties.getLogin();
        String accountKey = identifier.toLowerCase() + ":" + ip;
        addAttempt(loginEmailIpAttempts, accountKey, cfg.windowMillis());
        addAttempt(loginIpAttempts, ip, cfg.windowMillis());
    }

    public long loginRetryAfterSeconds() {
        return properties.getLogin().windowMillis() / 1000;
    }

    public int getLoginFailureResetHours() {
        return properties.getLogin().getFailureResetHours();
    }

    public int getLockoutAttempts() {
        return properties.getLogin().getLockoutAttempts();
    }

    public int getLockoutMinutes() {
        return properties.getLogin().getLockoutMinutes();
    }

    // ── Other auth endpoints: check+consume atomically (IP-only, in filter) ──

    public boolean checkRegister(String ip) {
        RateLimitProperties.Endpoint cfg = properties.getRegister();
        return checkAndRecord(registerAttempts, ip, cfg.getMaxAttempts(), cfg.windowMillis());
    }

    public boolean checkForgotPassword(String ip) {
        RateLimitProperties.Endpoint cfg = properties.getForgotPassword();
        return checkAndRecord(forgotPwdAttempts, ip, cfg.getMaxAttempts(), cfg.windowMillis());
    }

    public boolean checkResendVerification(String ip) {
        RateLimitProperties.Endpoint cfg = properties.getResendVerification();
        return checkAndRecord(resendVerifAttempts, ip, cfg.getMaxAttempts(), cfg.windowMillis());
    }

    public boolean checkRefresh(String ip) {
        RateLimitProperties.Endpoint cfg = properties.getRefresh();
        return checkAndRecord(refreshAttempts, ip, cfg.getMaxAttempts(), cfg.windowMillis());
    }

    /** Email-keyed: limits brute-force guesses against a specific account's 6-digit code. */
    public boolean checkVerifyEmail(String email) {
        RateLimitProperties.Endpoint cfg = properties.getVerifyEmail();
        return checkAndRecord(verifyEmailAttempts, email.toLowerCase(), cfg.getMaxAttempts(), cfg.windowMillis());
    }

    /** Email-keyed: limits brute-force guesses against a specific account's password reset code. */
    public boolean checkResetPassword(String email) {
        RateLimitProperties.Endpoint cfg = properties.getResetPassword();
        return checkAndRecord(resetPasswordAttempts, email.toLowerCase(), cfg.getMaxAttempts(), cfg.windowMillis());
    }

    /** Phone-keyed: limits brute-force guesses against a specific account's 6-digit SMS code. */
    public boolean checkVerifyPhone(String phoneNumber) {
        RateLimitProperties.Endpoint cfg = properties.getPhoneVerify();
        return checkAndRecord(phoneVerifyAttempts, phoneNumber, cfg.getMaxAttempts(), cfg.windowMillis());
    }

    /** Phone-keyed: limits how often a user can request a new SMS code. */
    public boolean checkResendPhoneVerification(String phoneNumber) {
        RateLimitProperties.Endpoint cfg = properties.getPhoneResend();
        return checkAndRecord(phoneResendAttempts, phoneNumber, cfg.getMaxAttempts(), cfg.windowMillis());
    }

    /** Member-keyed: limits how often a member can request a phone number update. */
    public boolean checkUpdatePhone(String memberId) {
        RateLimitProperties.Endpoint cfg = properties.getPhoneUpdate();
        return checkAndRecord(phoneUpdateAttempts, memberId, cfg.getMaxAttempts(), cfg.windowMillis());
    }

    public long verifyPhoneRetryAfterSeconds()  { return properties.getPhoneVerify().windowMillis() / 1000; }
    public long phoneResendRetryAfterSeconds()  { return properties.getPhoneResend().windowMillis() / 1000; }

    public long registerRetryAfterSeconds()        { return properties.getRegister().windowMillis() / 1000; }
    public long forgotPasswordRetryAfterSeconds()  { return properties.getForgotPassword().windowMillis() / 1000; }
    public long resendVerifRetryAfterSeconds()     { return properties.getResendVerification().windowMillis() / 1000; }
    public long refreshRetryAfterSeconds()         { return properties.getRefresh().windowMillis() / 1000; }
    public long verifyEmailRetryAfterSeconds()     { return properties.getVerifyEmail().windowMillis() / 1000; }
    public long resetPasswordRetryAfterSeconds()   { return properties.getResetPassword().windowMillis() / 1000; }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Check-only: returns true if the bucket is full. Does NOT add an entry. */
    private boolean isAtLimit(Map<String, Deque<Long>> store, String key, int max, long windowMs) {
        long now = System.currentTimeMillis();
        Deque<Long> q = store.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > windowMs) q.pollFirst();
            return q.size() >= max;
        }
    }

    /** Consume-only: adds one entry to the bucket regardless of its current size. */
    private void addAttempt(Map<String, Deque<Long>> store, String key, long windowMs) {
        long now = System.currentTimeMillis();
        Deque<Long> q = store.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > windowMs) q.pollFirst();
            q.addLast(now);
        }
    }

    /** Check-and-consume atomically: used by filter endpoints where the body is not needed. */
    private boolean checkAndRecord(Map<String, Deque<Long>> store, String key, int max, long windowMs) {
        long now = System.currentTimeMillis();
        Deque<Long> q = store.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > windowMs) q.pollFirst();
            if (q.size() >= max) return true;
            q.addLast(now);
        }
        return false;
    }

    @Scheduled(fixedDelay = 3_600_000L)
    void cleanup() {
        long now = System.currentTimeMillis();
        long maxWindow = Math.max(
                properties.getLogin().windowMillis(),
                Math.max(properties.getRegister().windowMillis(),
                Math.max(properties.getForgotPassword().windowMillis(),
                Math.max(properties.getResendVerification().windowMillis(),
                Math.max(properties.getRefresh().windowMillis(),
                Math.max(properties.getVerifyEmail().windowMillis(),
                Math.max(properties.getResetPassword().windowMillis(),
                Math.max(properties.getPhoneVerify().windowMillis(),
                Math.max(properties.getPhoneResend().windowMillis(),
                         properties.getPhoneUpdate().windowMillis())))))))));
        for (Map<String, Deque<Long>> store : List.of(
                loginEmailIpAttempts, loginIpAttempts,
                registerAttempts, forgotPwdAttempts, resendVerifAttempts, refreshAttempts,
                verifyEmailAttempts, resetPasswordAttempts,
                phoneVerifyAttempts, phoneResendAttempts, phoneUpdateAttempts)) {
            store.entrySet().removeIf(entry -> {
                Deque<Long> q = entry.getValue();
                synchronized (q) {
                    return q.isEmpty() || (now - q.peekFirst()) > maxWindow;
                }
            });
        }
    }
}
