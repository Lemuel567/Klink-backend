package com.example.demo.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class PaystackConfig {

    @Value("${paystack.secret-key:}")
    private String secretKey;

    @Value("${paystack.public-key:}")
    private String publicKey;

    @Value("${paystack.base-url:https://api.paystack.co}")
    private String baseUrl;

    @Value("${paystack.callback-url:}")
    private String callbackUrl;

    /** Paystack signs webhooks with the account secret key; this override is optional. */
    @Value("${paystack.webhook-secret:}")
    private String webhookSecret;

    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }

    /** The key used to verify X-Paystack-Signature (HMAC-SHA512). */
    public String signingKey() {
        return (webhookSecret != null && !webhookSecret.isBlank()) ? webhookSecret : secretKey;
    }
}
