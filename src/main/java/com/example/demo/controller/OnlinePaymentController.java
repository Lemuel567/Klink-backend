package com.example.demo.controller;

import com.example.demo.dto.request.InitiatePaymentRequest;
import com.example.demo.dto.response.InitiatePaymentResponse;
import com.example.demo.dto.response.OnlinePaymentResponse;
import com.example.demo.dto.response.PaymentSummaryResponse;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.OnlinePaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class OnlinePaymentController {

    private final OnlinePaymentService onlinePaymentService;

    // POST /api/v1/payments/initiate — any authenticated member starts an online payment
    @PostMapping("/initiate")
    public ResponseEntity<InitiatePaymentResponse> initiate(
            @Valid @RequestBody InitiatePaymentRequest request,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(onlinePaymentService.initiatePayment(request, principal));
    }

    // GET /api/v1/payments/verify/{reference} — verify with Paystack, record ledger entry on success
    @GetMapping("/verify/{reference}")
    public ResponseEntity<OnlinePaymentResponse> verify(
            @PathVariable String reference,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(onlinePaymentService.verifyAndCompletePayment(reference, principal));
    }

    // POST /api/v1/payments/webhook — Paystack calls this; auth = HMAC signature, not JWT
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Paystack-Signature", required = false) String signature) {
        onlinePaymentService.handleWebhook(rawBody, signature);
        return ResponseEntity.ok().build();
    }

    // GET /api/v1/payments/paystack/callback — where Paystack redirects the browser after payment.
    // Public, no data processed here; the app verifies via /verify/{reference}.
    @GetMapping(value = "/paystack/callback", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> callback() {
        return ResponseEntity.ok("""
                <!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Klink — Payment</title></head>
                <body style="font-family:sans-serif;background:#2D1B69;color:#fff;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;text-align:center">
                <div><div style="font-size:56px">🙏</div>
                <h2>Payment received</h2>
                <p>You can close this page and return to the Klink app.</p></div>
                </body></html>
                """);
    }

    // GET /api/v1/payments/history — members see their own; leadership sees all
    @GetMapping("/history")
    public ResponseEntity<Page<OnlinePaymentResponse>> history(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(onlinePaymentService.getHistory(principal, pageable));
    }

    // GET /api/v1/payments/summary — Pastor, Elder, Manager, Financial Secretary
    @GetMapping("/summary")
    public ResponseEntity<PaymentSummaryResponse> summary(
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(onlinePaymentService.getSummary(principal));
    }

    // GET /api/v1/payments/{id} — own payment, or any for privileged roles
    @GetMapping("/{id}")
    public ResponseEntity<OnlinePaymentResponse> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(onlinePaymentService.getPayment(id, principal));
    }
}
