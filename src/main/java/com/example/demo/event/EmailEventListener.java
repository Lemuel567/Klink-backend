package com.example.demo.event;

import com.example.demo.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVerificationEmail(VerificationEmailEvent event) {
        if (event.isPasswordReset()) {
            emailService.sendPasswordResetCode(event.getEmail(), event.getName(), event.getRawCode());
        } else {
            emailService.sendVerificationCode(event.getEmail(), event.getName(), event.getRawCode());
        }
    }
}
