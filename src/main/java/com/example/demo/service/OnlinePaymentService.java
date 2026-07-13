package com.example.demo.service;

import com.example.demo.dto.request.InitiatePaymentRequest;
import com.example.demo.dto.response.InitiatePaymentResponse;
import com.example.demo.dto.response.OnlinePaymentResponse;
import com.example.demo.dto.response.PaymentSummaryResponse;
import com.example.demo.event.OnlinePaymentSucceededEvent;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.security.RoleChecker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OnlinePaymentService {

    private final PaystackTransactionRepository paystackTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final ProjectContributionRepository projectContributionRepository;
    private final ChurchProjectRepository churchProjectRepository;
    private final MemberRepository memberRepository;
    private final ChurchRepository churchRepository;
    private final PaystackService paystackService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    private static final List<Role> PRIVILEGED = List.of(Role.PASTOR, Role.ELDER, Role.MANAGER, Role.FINANCIAL_SECRETARY);

    // ── Initiate ──────────────────────────────────────────────────────────────

    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, MemberPrincipal principal) {
        // 2026-07-12: a FINANCIAL_SECRETARY may initiate a payment ON BEHALF OF
        // another member (must be in the same church). Everyone else pays for self —
        // a stray memberId from a non-FinSec caller is ignored, never trusted.
        Member member = principal.getMember();
        if (request.getMemberId() != null
                && !request.getMemberId().equals(principal.getMemberId())
                && principal.getRole() == Role.FINANCIAL_SECRETARY) {
            member = memberRepository.findByChurchIdAndId(principal.getChurchId(), request.getMemberId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        }

        String email = member.getEmail();
        if (email == null || email.isBlank()) {
            // Paystack requires an email per transaction; synthesize a stable one from the member id
            email = "member-" + member.getId() + "@klink.app";
        }

        if (request.getPaymentType() == OnlinePaymentType.PROJECT_CONTRIBUTION) {
            if (request.getProjectId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "projectId is required for a project contribution");
            }
            ChurchProject project = churchProjectRepository
                    .findByChurchIdAndIdAndDeletedAtIsNull(principal.getChurchId(), request.getProjectId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
            if (project.getStatus() == ProjectStatus.COMPLETED || project.getStatus() == ProjectStatus.CANCELLED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "This project is " + project.getStatus().name().toLowerCase() + " and no longer accepts contributions");
            }
        }

        BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        String reference = "KLINK-" + UUID.randomUUID();

        PaystackService.InitializedTransaction init = paystackService.initializeTransaction(
                email, amount, reference,
                principal.getChurchId(), member.getId(),
                request.getPaymentType().name(), request.getProjectId());

        // Transaction belongs to the GIVER (member), not the FinSec who initiated it —
        // the ledger record, receipt, and history all credit the right person.
        PaystackTransaction tx = PaystackTransaction.builder()
                .churchId(principal.getChurchId())
                .memberId(member.getId())
                .amount(amount)
                .paymentType(request.getPaymentType())
                .paystackReference(reference)
                .customerEmail(email)
                .description(request.getDescription())
                .projectId(request.getProjectId())
                .build();
        paystackTransactionRepository.save(tx);

        auditLogService.onlinePaymentInitiated(principal.getMemberId(), reference, amount, request.getPaymentType().name());

        return InitiatePaymentResponse.builder()
                .authorizationUrl(init.authorizationUrl())
                .reference(reference)
                .amount(amount)
                .currency("GHS")
                .build();
    }

    // ── Verify + complete ─────────────────────────────────────────────────────

    public OnlinePaymentResponse verifyAndCompletePayment(String reference, MemberPrincipal principal) {
        PaystackTransaction tx = paystackTransactionRepository.findByPaystackReference(reference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        // church isolation + members may only verify their own payments
        if (!tx.getChurchId().equals(principal.getChurchId())
                || (!tx.getMemberId().equals(principal.getMemberId()) && !isPrivileged(principal))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        }

        return toResponse(completeIfPaid(tx.getPaystackReference()));
    }

    /**
     * Shared by the authenticated verify endpoint and the webhook. Idempotent AND
     * concurrency-safe: the row is re-read under SELECT FOR UPDATE so two simultaneous
     * verify/webhook calls can never both materialise the ledger record.
     */
    private PaystackTransaction completeIfPaid(String reference) {
        PaystackTransaction tx = paystackTransactionRepository.findByPaystackReferenceForUpdate(reference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (tx.getStatus() == OnlinePaymentStatus.SUCCESS && tx.isRecorded()) return tx; // already done

        PaystackService.VerifiedTransaction verified = paystackService.verifyTransaction(tx.getPaystackReference());

        switch (verified.status()) {
            case "success" -> {
                // Paystack is the source of truth for the captured amount
                tx.setAmount(verified.amountGhs());
                tx.setStatus(OnlinePaymentStatus.SUCCESS);
                tx.setChannel(verified.channel());
                tx.setPaystackTransactionId(verified.transactionId());
                tx.setPaystackAuthorizationCode(verified.authorizationCode());
                if (tx.getPaidAt() == null) tx.setPaidAt(LocalDateTime.now());

                if (!tx.isRecorded()) {
                    materialiseLedgerRecord(tx);
                    tx.setRecorded(true);
                    eventPublisher.publishEvent(new OnlinePaymentSucceededEvent(this, tx.getId()));
                }
            }
            case "failed" -> tx.setStatus(OnlinePaymentStatus.FAILED);
            case "abandoned" -> tx.setStatus(OnlinePaymentStatus.ABANDONED);
            default -> { /* still pending — leave as is */ }
        }
        paystackTransactionRepository.save(tx);
        auditLogService.onlinePaymentCompleted(tx.getPaystackReference(), tx.getStatus().name(), tx.getAmount());
        return tx;
    }

    /** Creates the church ledger record for a successful online payment. */
    private void materialiseLedgerRecord(PaystackTransaction tx) {
        Member member = memberRepository.findByChurchIdAndId(tx.getChurchId(), tx.getMemberId()).orElse(null);
        Church church = churchRepository.findById(tx.getChurchId()).orElse(null);
        if (member == null || church == null) {
            log.error("Cannot record online payment {}: member or church missing", tx.getPaystackReference());
            return;
        }

        if (tx.getPaymentType() == OnlinePaymentType.PROJECT_CONTRIBUTION && tx.getProjectId() != null) {
            ChurchProject project = churchProjectRepository
                    .findByChurchIdAndIdAndDeletedAtIsNull(tx.getChurchId(), tx.getProjectId())
                    .orElse(null);
            if (project != null) {
                ProjectContribution contribution = ProjectContribution.builder()
                        .project(project)
                        .member(member)
                        .church(church)
                        .amount(tx.getAmount())
                        .contributionDate(LocalDate.now())
                        .paymentMethod(channelToMethod(tx.getChannel()))
                        .recordedBy(tx.getMemberId())
                        .notes("Paystack " + tx.getPaystackReference())
                        .build();
                projectContributionRepository.save(contribution);

                // amountRaised is always recalculated from SUM, never incremented
                project.setAmountRaised(projectContributionRepository.sumAmountByProjectId(project.getId()));
                churchProjectRepository.save(project);
                return;
            }
            log.error("Project {} missing for paid transaction {} — recording as special contribution",
                    tx.getProjectId(), tx.getPaystackReference());
        }

        Payment payment = Payment.builder()
                .church(church)
                .member(member)
                .paymentType(ledgerType(tx.getPaymentType()))
                .amount(tx.getAmount())
                .paymentMonth(YearMonth.now().toString())
                .paymentDate(LocalDate.now())
                .status(PaymentStatus.CONFIRMED)
                .momoReference(tx.getPaystackReference())
                .recordedBy(tx.getMemberId())
                .build();
        paymentRepository.save(payment);
    }

    private PaymentType ledgerType(OnlinePaymentType type) {
        return switch (type) {
            case TITHE -> PaymentType.TITHE;
            case OFFERING -> PaymentType.OFFERING;
            case WELFARE -> PaymentType.WELFARE;
            default -> PaymentType.SPECIAL_CONTRIBUTION;
        };
    }

    private ContributionPaymentMethod channelToMethod(String channel) {
        if (channel != null && channel.toLowerCase().contains("mobile")) {
            return ContributionPaymentMethod.MOBILE_MONEY;
        }
        return ContributionPaymentMethod.BANK_TRANSFER;
    }

    // ── Webhook ───────────────────────────────────────────────────────────────

    public void handleWebhook(String rawBody, String signature) {
        if (!paystackService.isValidWebhookSignature(rawBody, signature)) {
            auditLogService.webhookSignatureRejected("paystack");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid signature");
        }

        try {
            JsonNode event = objectMapper.readTree(rawBody);
            String eventType = event.path("event").asText("");
            auditLogService.webhookReceived(eventType, event.path("data").path("reference").asText(""));

            if ("charge.success".equals(eventType)) {
                String reference = event.path("data").path("reference").asText(null);
                if (reference != null) {
                    if (paystackTransactionRepository.findByPaystackReference(reference).isPresent()) {
                        completeIfPaid(reference); // re-reads under SELECT FOR UPDATE
                    } else {
                        log.warn("Webhook for unknown reference {}", reference);
                    }
                }
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            // Never bubble processing errors back to Paystack — signature was valid, ack it
            log.error("Webhook processing error: {}", e.getMessage());
        }
    }

    // ── History / detail / summary ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<OnlinePaymentResponse> getHistory(MemberPrincipal principal, Pageable pageable) {
        UUID churchId = principal.getChurchId();
        Page<PaystackTransaction> page = isPrivileged(principal)
                ? paystackTransactionRepository.findByChurchIdAndDeletedAtIsNullOrderByCreatedAtDesc(churchId, pageable)
                : paystackTransactionRepository.findByChurchIdAndMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        churchId, principal.getMemberId(), pageable);

        Map<UUID, String> names = resolveNames(churchId, page.getContent());
        return page.map(t -> OnlinePaymentResponse.from(t, names.get(t.getMemberId())));
    }

    @Transactional(readOnly = true)
    public OnlinePaymentResponse getPayment(UUID id, MemberPrincipal principal) {
        PaystackTransaction tx = paystackTransactionRepository.findById(id)
                .filter(t -> t.getChurchId().equals(principal.getChurchId()) && t.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (!tx.getMemberId().equals(principal.getMemberId()) && !isPrivileged(principal)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        }
        return toResponse(tx);
    }

    @Transactional(readOnly = true)
    public PaymentSummaryResponse getSummary(MemberPrincipal principal) {
        RoleChecker.require(principal, "Only leadership can view the payment summary",
                Role.PASTOR, Role.ELDER, Role.MANAGER, Role.FINANCIAL_SECRETARY);

        UUID churchId = principal.getChurchId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime yearStart = LocalDate.of(now.getYear(), 1, 1).atStartOfDay();

        Map<String, Long> byType = new LinkedHashMap<>();
        for (OnlinePaymentType type : OnlinePaymentType.values()) {
            long count = paystackTransactionRepository
                    .countByChurchIdAndStatusAndPaymentTypeAndDeletedAtIsNull(churchId, OnlinePaymentStatus.SUCCESS, type);
            if (count > 0) byType.put(type.name(), count);
        }

        List<PaystackTransaction> recent = paystackTransactionRepository
                .findTop10ByChurchIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(churchId, OnlinePaymentStatus.SUCCESS);
        Map<UUID, String> names = resolveNames(churchId, recent);

        return PaymentSummaryResponse.builder()
                .totalThisMonth(paystackTransactionRepository.sumAmountByChurchIdAndStatusBetween(
                        churchId, OnlinePaymentStatus.SUCCESS, monthStart, now))
                .totalThisYear(paystackTransactionRepository.sumAmountByChurchIdAndStatusBetween(
                        churchId, OnlinePaymentStatus.SUCCESS, yearStart, now))
                .successCount(paystackTransactionRepository.countByChurchIdAndStatusAndDeletedAtIsNull(churchId, OnlinePaymentStatus.SUCCESS))
                .pendingCount(paystackTransactionRepository.countByChurchIdAndStatusAndDeletedAtIsNull(churchId, OnlinePaymentStatus.PENDING))
                .failedCount(paystackTransactionRepository.countByChurchIdAndStatusAndDeletedAtIsNull(churchId, OnlinePaymentStatus.FAILED))
                .countByPaymentType(byType)
                .mobileMoneyCount(paystackTransactionRepository.countByChurchIdAndStatusAndChannelAndDeletedAtIsNull(
                        churchId, OnlinePaymentStatus.SUCCESS, "mobile_money"))
                .cardCount(paystackTransactionRepository.countByChurchIdAndStatusAndChannelAndDeletedAtIsNull(
                        churchId, OnlinePaymentStatus.SUCCESS, "card"))
                .recentPayments(recent.stream()
                        .map(t -> OnlinePaymentResponse.from(t, names.get(t.getMemberId())))
                        .collect(Collectors.toList()))
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isPrivileged(MemberPrincipal principal) {
        return PRIVILEGED.contains(principal.getRole());
    }

    private OnlinePaymentResponse toResponse(PaystackTransaction tx) {
        String name = memberRepository.findByChurchIdAndId(tx.getChurchId(), tx.getMemberId())
                .map(Member::getFullName).orElse(null);
        return OnlinePaymentResponse.from(tx, name);
    }

    private Map<UUID, String> resolveNames(UUID churchId, List<PaystackTransaction> txs) {
        List<UUID> ids = txs.stream().map(PaystackTransaction::getMemberId).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) return Map.of();
        return memberRepository.findByChurchIdAndIdIn(churchId, ids).stream()
                .collect(Collectors.toMap(Member::getId, Member::getFullName, (a, b) -> a));
    }
}
