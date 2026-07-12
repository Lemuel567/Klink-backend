package com.example.demo.model;

/**
 * What an online (Paystack) payment is for. Distinct from {@link PaymentType},
 * which classifies the ledger rows in the payments table.
 */
public enum OnlinePaymentType {
    TITHE, OFFERING, WELFARE, BUILDING_FUND, MISSIONS, PROJECT_CONTRIBUTION, OTHER
}
