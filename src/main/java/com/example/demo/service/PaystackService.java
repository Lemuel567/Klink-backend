package com.example.demo.service;

import com.example.demo.config.PaystackConfig;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Thin client for the Paystack REST API. Amounts are converted GHS → pesewas
 * here and nowhere else; the database always stores GHS.
 */
@Service
@Slf4j
public class PaystackService {

    private final PaystackConfig config;
    private final RestClient restClient;

    public PaystackService(PaystackConfig config, RestClient.Builder restClientBuilder) {
        this.config = config;
        this.restClient = restClientBuilder.baseUrl(config.getBaseUrl()).build();
    }

    /** Result of POST /transaction/initialize. */
    public record InitializedTransaction(String authorizationUrl, String accessCode, String reference) {}

    public InitializedTransaction initializeTransaction(String email,
                                                        BigDecimal amountGhs,
                                                        String reference,
                                                        UUID churchId,
                                                        UUID memberId,
                                                        String paymentType,
                                                        UUID projectId) {
        requireConfigured();

        long amountPesewas = amountGhs.setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("churchId", churchId.toString());
        metadata.put("memberId", memberId.toString());
        metadata.put("paymentType", paymentType);
        if (projectId != null) metadata.put("projectId", projectId.toString());

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("amount", amountPesewas);
        body.put("currency", "GHS");
        body.put("reference", reference);
        body.put("metadata", metadata);
        if (config.getCallbackUrl() != null && !config.getCallbackUrl().isBlank()) {
            body.put("callback_url", config.getCallbackUrl());
        }

        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/transaction/initialize")
                    .header("Authorization", "Bearer " + config.getSecretKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            log.error("Paystack initialize failed for reference {}: {}", reference, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Payment service is unavailable. Please try again.");
        }

        if (response == null || !response.path("status").asBoolean(false)) {
            log.error("Paystack initialize rejected for reference {}: {}", reference,
                    response == null ? "empty response" : response.path("message").asText());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Payment could not be initialised. Please try again.");
        }

        JsonNode data = response.path("data");
        return new InitializedTransaction(
                data.path("authorization_url").asText(),
                data.path("access_code").asText(),
                data.path("reference").asText(reference));
    }

    /** Result of GET /transaction/verify/{reference}. */
    public record VerifiedTransaction(String status,          // success | failed | abandoned | pending
                                      BigDecimal amountGhs,
                                      String channel,
                                      String transactionId,
                                      String authorizationCode,
                                      String paidAt) {}

    public VerifiedTransaction verifyTransaction(String reference) {
        requireConfigured();

        JsonNode response;
        try {
            response = restClient.get()
                    .uri("/transaction/verify/{reference}", reference)
                    .header("Authorization", "Bearer " + config.getSecretKey())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            log.error("Paystack verify failed for reference {}: {}", reference, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not verify the payment. Please try again.");
        }

        if (response == null || !response.path("status").asBoolean(false)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment reference not found");
        }

        JsonNode data = response.path("data");
        BigDecimal amountGhs = BigDecimal.valueOf(data.path("amount").asLong(0))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return new VerifiedTransaction(
                data.path("status").asText(""),
                amountGhs,
                data.path("channel").asText(null),
                data.path("id").isMissingNode() ? null : data.path("id").asText(),
                data.path("authorization").path("authorization_code").asText(null),
                data.path("paid_at").asText(null));
    }

    /** Verifies X-Paystack-Signature: HMAC-SHA512 of the raw body with the secret key. */
    public boolean isValidWebhookSignature(String rawBody, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank() || !config.isConfigured()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(config.signingKey().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] digest = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format("%02x", b));
            // constant-time comparison
            return java.security.MessageDigest.isEqual(
                    hex.toString().getBytes(StandardCharsets.UTF_8),
                    signatureHeader.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private void requireConfigured() {
        if (!config.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Online payments are not configured for this server");
        }
    }
}
