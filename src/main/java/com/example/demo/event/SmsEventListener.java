package com.example.demo.event;

import com.example.demo.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmsEventListener {

    private final SmsService smsService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSmsVerification(SmsVerificationEvent event) {
        try {
            smsService.sendVerificationCode(event.getPhoneNumber(), event.getName(), event.getRawCode());
        } catch (Exception e) {
            log.error("[SMS] Async send failed to=...{} reason={}", last4(event.getPhoneNumber()), e.getMessage());
        }
    }

    private String last4(String phone) {
        if (phone == null || phone.length() < 4) return "????";
        return phone.substring(phone.length() - 4);
    }
}
