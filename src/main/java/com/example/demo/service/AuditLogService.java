package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class AuditLogService {

    public void loginSuccess(UUID memberId, String email, String ip) {
        log.info("[AUTH] LOGIN_SUCCESS memberId={} email={} ip={}", memberId, email, ip);
    }

    public void loginFailure(String email, String ip) {
        log.warn("[AUTH] LOGIN_FAILURE email={} ip={}", email, ip);
    }

    public void accountLocked(String email, String ip) {
        log.warn("[AUTH] ACCOUNT_LOCKED email={} ip={}", email, ip);
    }

    public void logout(UUID memberId, String ip) {
        log.info("[AUTH] LOGOUT memberId={} ip={}", memberId, ip);
    }

    public void tokenRefreshed(UUID memberId, String ip) {
        log.info("[AUTH] TOKEN_REFRESH memberId={} ip={}", memberId, ip);
    }

    public void passwordChanged(UUID memberId, String ip) {
        log.info("[AUTH] PASSWORD_CHANGE memberId={} ip={}", memberId, ip);
    }

    public void passwordReset(String email, String ip) {
        log.info("[AUTH] PASSWORD_RESET email={} ip={}", email, ip);
    }

    public void registrationSuccess(UUID memberId, String email, String ip) {
        log.info("[AUTH] REGISTER memberId={} email={} ip={}", memberId, email, ip);
    }

    public void tokenReuseDetected(UUID memberId, String ip) {
        log.warn("[AUTH] TOKEN_REUSE_DETECTED memberId={} ip={} — entire token family revoked", memberId, ip);
    }

    public void phoneVerificationSent(UUID memberId, String phoneNumber, String ip) {
        log.info("[AUTH] PHONE_VERIFICATION_SENT memberId={} phone=...{} ip={}", memberId, last4(phoneNumber), ip);
    }

    public void phoneVerificationSuccess(UUID memberId, String phoneNumber) {
        log.info("[AUTH] PHONE_VERIFICATION_SUCCESS memberId={} phone=...{}", memberId, last4(phoneNumber));
    }

    public void phoneVerificationFailed(UUID memberId, String phoneNumber) {
        log.warn("[AUTH] PHONE_VERIFICATION_FAILED memberId={} phone=...{}", memberId, last4(phoneNumber));
    }

    public void phoneVerificationExpired(UUID memberId, String phoneNumber) {
        log.warn("[AUTH] PHONE_VERIFICATION_EXPIRED memberId={} phone=...{}", memberId, last4(phoneNumber));
    }

    public void phoneVerificationLocked(UUID memberId, String phoneNumber) {
        log.warn("[AUTH] PHONE_VERIFICATION_LOCKED memberId={} phone=...{}", memberId, last4(phoneNumber));
    }

    public void phoneNumberAdded(UUID memberId, String phoneNumber, String ip) {
        log.info("[AUTH] PHONE_NUMBER_ADDED memberId={} phone=...{} ip={}", memberId, last4(phoneNumber), ip);
    }

    public void phoneNumberChanged(UUID memberId, String phoneNumber, String ip) {
        log.info("[AUTH] PHONE_NUMBER_CHANGED memberId={} phone=...{} ip={}", memberId, last4(phoneNumber), ip);
    }

    public void loginViaPhone(UUID memberId, String phoneNumber, String ip) {
        log.info("[AUTH] LOGIN_VIA_PHONE memberId={} phone=...{} ip={}", memberId, last4(phoneNumber), ip);
    }

    public void memberLeftChurch(UUID memberId, UUID churchId) {
        log.warn("[MEMBER] LEFT_CHURCH memberId={} churchId={} (self-deactivated)", memberId, churchId);
    }

    // Facilities

    public void facilityCreated(UUID actorId, UUID facilityId, String name) {
        log.info("[FACILITY] CREATED actorId={} facilityId={} name={}", actorId, facilityId, name);
    }

    public void facilityUpdated(UUID actorId, UUID facilityId) {
        log.info("[FACILITY] UPDATED actorId={} facilityId={}", actorId, facilityId);
    }

    public void facilityDeleted(UUID actorId, UUID facilityId) {
        log.warn("[FACILITY] DELETED actorId={} facilityId={}", actorId, facilityId);
    }

    // Projects

    public void projectCreated(UUID actorId, UUID projectId, String title) {
        log.info("[PROJECT] CREATED actorId={} projectId={} title={}", actorId, projectId, title);
    }

    public void projectStatusChanged(UUID actorId, UUID projectId, String from, String to) {
        log.info("[PROJECT] STATUS_CHANGE actorId={} projectId={} from={} to={}", actorId, projectId, from, to);
    }

    public void projectDeleted(UUID actorId, UUID projectId) {
        log.warn("[PROJECT] DELETED actorId={} projectId={}", actorId, projectId);
    }

    public void contributionRecorded(UUID actorId, UUID projectId, UUID memberId, java.math.BigDecimal amount) {
        log.info("[PROJECT] CONTRIBUTION actorId={} projectId={} memberId={} amount={}", actorId, projectId, memberId, amount);
    }

    // Online payments (Paystack)

    public void onlinePaymentInitiated(UUID memberId, String reference, java.math.BigDecimal amount, String type) {
        log.info("[PAYSTACK] INITIATED memberId={} reference={} amount={} type={}", memberId, reference, amount, type);
    }

    public void onlinePaymentCompleted(String reference, String status, java.math.BigDecimal amount) {
        log.info("[PAYSTACK] COMPLETED reference={} status={} amount={}", reference, status, amount);
    }

    public void webhookSignatureRejected(String remoteInfo) {
        log.warn("[PAYSTACK] WEBHOOK_SIGNATURE_REJECTED source={}", remoteInfo);
    }

    public void webhookReceived(String eventType, String reference) {
        log.info("[PAYSTACK] WEBHOOK event={} reference={}", eventType, reference);
    }

    // Media uploads

    public void mediaUploaded(UUID actorId, String folder, String url) {
        log.info("[MEDIA] UPLOADED actorId={} folder={} url={}", actorId, folder, url);
    }

    private String last4(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) return "????";
        return phoneNumber.substring(phoneNumber.length() - 4);
    }
}
