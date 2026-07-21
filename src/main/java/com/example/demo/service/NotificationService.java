package com.example.demo.service;

import com.example.demo.model.Member;
import com.example.demo.model.MemberStatus;
import com.example.demo.repository.MemberRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final MemberRepository memberRepository;
    private final SmsService smsService;

    private final RestClient restClient = RestClient.create();

    @Value("${sms.api-url}")
    private String smsApiUrl;

    @Value("${sms.api-key}")
    private String smsApiKey;

    @Value("${sms.sender-id}")
    private String smsSenderId;

    // Returns true if a notification was successfully dispatched to this member.
    public boolean notifyMember(Member member, String title, String body) {
        if (member.getStatus() != MemberStatus.ACTIVE) return false;

        if (member.isHasSmartphone()
                && member.getFcmToken() != null && !member.getFcmToken().isBlank()) {
            if (sendPushNotification(member.getFcmToken(), title, body)) {
                return true;
            }
        }
        // SMS: the primary channel for non-smartphone members AND the fallback for
        // smartphone members with no (or dead) push token — previously those
        // members received nothing at all. Prefer the verified E.164 number.
        String phone = (member.getPhoneNumber() != null && !member.getPhoneNumber().isBlank())
                ? member.getPhoneNumber()
                : member.getPhone();
        if (phone != null && !phone.isBlank()) {
            return sendSms(phone, title + ": " + body);
        }
        return false;
    }

    // Paginates through all active members in a church 200 at a time.
    // Returns the number of notifications successfully dispatched.
    public int notifyAllMembers(UUID churchId, String title, String body) {
        int count = 0;
        Pageable pageable = PageRequest.of(0, 200);
        Page<Member> page;
        do {
            page = memberRepository.findByChurchIdAndStatus(churchId, MemberStatus.ACTIVE, pageable);
            for (Member member : page.getContent()) {
                if (notifyMember(member, title, body)) count++;
            }
            pageable = pageable.next();
        } while (page.hasNext());
        return count;
    }

    // Returns the number of notifications successfully dispatched.
    public int notifyMembers(List<Member> members, String title, String body) {
        int count = 0;
        for (Member member : members) {
            if (notifyMember(member, title, body)) count++;
        }
        return count;
    }

    // Returns true if the push was accepted by Firebase; false on any failure.
    private boolean sendPushNotification(String fcmToken, String title, String body) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized — skipping push notification");
            return false;
        }
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();
            FirebaseMessaging.getInstance().send(message);
            log.debug("Push notification sent successfully");
            return true;
        } catch (Exception e) {
            log.error("Failed to send push notification: {}", e.getMessage());
            return false;
        }
    }

    // Returns true if the SMS was accepted by the gateway; false on any failure.
    // Falls back to Twilio (the verification-SMS provider) when no dedicated
    // notification gateway is configured — non-smartphone members must not be silently skipped.
    private boolean sendSms(String phoneNumber, String message) {
        if (smsApiUrl == null || smsApiUrl.isBlank()) {
            return smsService.sendMessage(phoneNumber, message);
        }
        try {
            restClient.post()
                    .uri(smsApiUrl)
                    .header("Authorization", "Bearer " + smsApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "recipient", phoneNumber,
                            "sender", smsSenderId,
                            "message", message
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("SMS sent successfully");
            return true;
        } catch (Exception e) {
            log.error("Failed to send SMS: {}", e.getMessage());
            return false;
        }
    }
}