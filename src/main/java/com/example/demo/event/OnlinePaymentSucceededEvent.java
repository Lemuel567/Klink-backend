package com.example.demo.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Published after a Paystack payment is confirmed and its ledger record
 * committed. Handled AFTER_COMMIT: receipt email + notifications.
 */
@Getter
public class OnlinePaymentSucceededEvent extends ApplicationEvent {

    private final UUID transactionId;

    public OnlinePaymentSucceededEvent(Object source, UUID transactionId) {
        super(source);
        this.transactionId = transactionId;
    }
}
