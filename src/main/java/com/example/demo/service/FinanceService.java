package com.example.demo.service;

import com.example.demo.dto.request.RecordOfferingRequest;
import com.example.demo.dto.request.RecordTitheRequest;
import com.example.demo.dto.request.RecordWelfareRequest;
import com.example.demo.dto.response.CollectionsSummaryResponse;
import com.example.demo.dto.response.MemberResponse;
import com.example.demo.dto.response.PaymentResponse;
import com.example.demo.model.*;
import com.example.demo.repository.ChurchRepository;
import com.example.demo.repository.CollectionTotal;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.security.RoleChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class FinanceService {

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final ChurchRepository churchRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public PaymentResponse recordOffering(RecordOfferingRequest request, MemberPrincipal principal) {
        RoleChecker.requireFinancialSecretary(principal);

        // Only a prior MANUAL lump sum (member null) is a duplicate. Members'
        // own online offerings that day are member-attributed and must not block
        // the secretary recording the counted cash.
        boolean alreadyRecorded = paymentRepository.existsByChurchIdAndPaymentTypeAndPaymentDateAndMemberIsNull(
                principal.getChurchId(), PaymentType.OFFERING, request.getServiceDate());
        if (alreadyRecorded) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A cash offering has already been recorded for this date. If you need to correct it, contact your administrator.");
        }

        String paymentMonth = request.getServiceDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        Payment payment = Payment.builder()
                .church(principal.getMember().getChurch())
                .paymentType(PaymentType.OFFERING)
                .amount(request.getAmount())
                .paymentMonth(paymentMonth)
                .paymentDate(request.getServiceDate())
                .status(PaymentStatus.CONFIRMED)
                .recordedBy(principal.getMemberId())
                .build();

        return PaymentResponse.from(paymentRepository.save(payment));
    }

    public PaymentResponse recordTithe(RecordTitheRequest request, MemberPrincipal principal) {
        RoleChecker.requireFinancialSecretary(principal);

        Member member = memberRepository.findByChurchIdAndId(principal.getChurchId(), request.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        Payment payment = Payment.builder()
                .church(principal.getMember().getChurch())
                .member(member)
                .paymentType(PaymentType.TITHE)
                .amount(request.getAmount())
                .paymentMonth(request.getPaymentMonth())
                .paymentDate(request.getPaymentDate())
                .momoReference(request.getMomoReference())
                .status(PaymentStatus.CONFIRMED)
                .recordedBy(principal.getMemberId())
                .build();

        return PaymentResponse.from(paymentRepository.save(payment));
    }

    public List<PaymentResponse> recordWelfare(RecordWelfareRequest request, MemberPrincipal principal) {
        RoleChecker.requireFinancialSecretary(principal);

        Member member = memberRepository.findByChurchIdAndId(principal.getChurchId(), request.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        Church church = churchRepository.findById(principal.getChurchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Church not found"));
        BigDecimal monthlyAmount = church.getWelfareAmount();

        if (monthlyAmount == null || monthlyAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Church welfare amount has not been configured. The Pastor must set it first.");
        }

        // Welfare is a fixed monthly amount. The amount paid must be an exact multiple —
        // anything else silently loses money (a short payment must not buy a full month,
        // and an overpayment remainder must not vanish from the books).
        BigDecimal[] division = request.getAmountPaid().divideAndRemainder(monthlyAmount);
        if (division[1].compareTo(BigDecimal.ZERO) != 0 || division[0].compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Welfare payments must be an exact multiple of the monthly amount ("
                            + monthlyAmount.toPlainString() + "). Received: "
                            + request.getAmountPaid().toPlainString());
        }
        int monthsCovered = division[0].intValue();

        YearMonth startMonth = YearMonth.parse(request.getPaymentMonth());

        // Every month the cash covers must currently be unpaid. Skipping paid
        // months silently swallowed their share of the money — the same
        // "money vanishes" failure the exact-multiple rule exists to prevent.
        List<String> alreadyPaidMonths = new ArrayList<>();
        for (int i = 0; i < monthsCovered; i++) {
            String monthStr = startMonth.plusMonths(i).toString();
            boolean alreadyPaid = !paymentRepository.findByChurchIdAndMemberIdAndPaymentTypeAndPaymentMonth(
                    principal.getChurchId(), request.getMemberId(),
                    PaymentType.WELFARE, monthStr).isEmpty();
            if (alreadyPaid) alreadyPaidMonths.add(monthStr);
        }
        if (!alreadyPaidMonths.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Welfare is already recorded for " + String.join(", ", alreadyPaidMonths)
                            + ". Adjust the amount to cover only unpaid months.");
        }

        List<PaymentResponse> responses = new ArrayList<>();
        for (int i = 0; i < monthsCovered; i++) {
            String monthStr = startMonth.plusMonths(i).toString();
            Payment payment = Payment.builder()
                    .church(church)
                    .member(member)
                    .paymentType(PaymentType.WELFARE)
                    .amount(monthlyAmount)
                    .paymentMonth(monthStr)
                    .paymentDate(request.getPaymentDate())
                    .momoReference(request.getMomoReference())
                    .status(PaymentStatus.CONFIRMED)
                    .recordedBy(principal.getMemberId())
                    .build();
            responses.add(PaymentResponse.from(paymentRepository.save(payment)));
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public Page<MemberResponse> getWelfareDefaulters(String paymentMonth, MemberPrincipal principal, Pageable pageable) {
        RoleChecker.requireFinancialSecretaryOrPrivileged(principal);

        return memberRepository.findWelfareDefaultersPaged(
                        principal.getChurchId(), paymentMonth,
                        MemberStatus.ACTIVE, PaymentType.WELFARE, pageable)
                .map(MemberResponse::from);
    }

    public void sendManualWelfareReminder(String paymentMonth, MemberPrincipal principal) {
        RoleChecker.requireFinancialSecretary(principal);

        List<Member> defaulters = memberRepository.findWelfareDefaulters(
                principal.getChurchId(), paymentMonth,
                MemberStatus.ACTIVE, PaymentType.WELFARE);

        if (defaulters.isEmpty()) return;

        // AFTER_COMMIT event — sending hundreds of push/SMS must not block the request thread
        eventPublisher.publishEvent(new com.example.demo.event.TargetedNotificationEvent(
                this, principal.getChurchId(),
                defaulters.stream().map(Member::getId).toList(),
                "Welfare Reminder",
                "You have not paid your welfare for " + paymentMonth + ". Please pay as soon as possible."));
    }

    /**
     * Reconcile church collections over a period: for each type, the cash
     * recorded by hand plus what came in automatically through the app, and the
     * combined totals. Null dates default to the current month; a single date
     * (from == to) reconciles one service day.
     */
    @Transactional(readOnly = true)
    public CollectionsSummaryResponse getCollectionsSummary(LocalDate from, LocalDate to, MemberPrincipal principal) {
        RoleChecker.requireFinancialSecretaryOrPrivileged(principal);

        if (from == null && to == null) {
            YearMonth ym = YearMonth.now();
            from = ym.atDay(1);
            to = ym.atEndOfMonth();
        } else if (from == null) {
            from = to;
        } else if (to == null) {
            to = from;
        }
        if (to.isBefore(from)) {
            LocalDate swap = from;
            from = to;
            to = swap;
        }

        Map<PaymentType, CollectionTotal> byType = new EnumMap<>(PaymentType.class);
        for (CollectionTotal ct : paymentRepository.summariseCollections(principal.getChurchId(), from, to)) {
            byType.put(ct.getType(), ct);
        }

        // Church-money types in a familiar order (group DUES excluded by the query)
        List<PaymentType> order = List.of(
                PaymentType.OFFERING, PaymentType.TITHE, PaymentType.WELFARE, PaymentType.SPECIAL_CONTRIBUTION);

        List<CollectionsSummaryResponse.Line> lines = new ArrayList<>();
        BigDecimal manualTotal = BigDecimal.ZERO;
        BigDecimal onlineTotal = BigDecimal.ZERO;
        for (PaymentType type : order) {
            CollectionTotal ct = byType.get(type);
            BigDecimal manual = ct != null && ct.getManualTotal() != null ? ct.getManualTotal() : BigDecimal.ZERO;
            BigDecimal online = ct != null && ct.getOnlineTotal() != null ? ct.getOnlineTotal() : BigDecimal.ZERO;
            lines.add(CollectionsSummaryResponse.Line.builder()
                    .type(type.name())
                    .manual(manual)
                    .online(online)
                    .total(manual.add(online))
                    .build());
            manualTotal = manualTotal.add(manual);
            onlineTotal = onlineTotal.add(online);
        }

        return CollectionsSummaryResponse.builder()
                .from(from)
                .to(to)
                .lines(lines)
                .manualTotal(manualTotal)
                .onlineTotal(onlineTotal)
                .grandTotal(manualTotal.add(onlineTotal))
                .build();
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getMyFinances(MemberPrincipal principal, Pageable pageable) {
        return paymentRepository.findByChurchIdAndMemberIdAndPaymentTypeIn(
                        principal.getChurchId(), principal.getMemberId(),
                        List.of(PaymentType.TITHE, PaymentType.WELFARE),
                        pageable)
                .map(PaymentResponse::from);
    }

}
