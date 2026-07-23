package com.example.demo.repository;

import com.example.demo.model.PaymentType;

import java.math.BigDecimal;

/**
 * Projection for the service-collections summary: per payment type, the total
 * recorded by hand (no reference) vs. the total taken automatically through the
 * app (carries a Paystack reference).
 */
public interface CollectionTotal {
    PaymentType getType();
    BigDecimal getManualTotal();
    BigDecimal getOnlineTotal();
}
