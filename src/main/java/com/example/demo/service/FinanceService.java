package com.example.demo.service;

import com.example.demo.dto.request.RecordOfferingRequest;
import com.example.demo.dto.request.RecordTitheRequest;
import com.example.demo.dto.request.RecordWelfareRequest;
import com.example.demo.dto.response.MemberResponse;
import com.example.demo.dto.response.PaymentResponse;
import com.example.demo.model.*;
import com.example.demo.repository.ChurchRepository;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FinanceService {

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final ChurchRepository churchRepository;
    private final NotificationService notificationService;

    public PaymentResponse recordOffering(RecordOfferingRequest request, MemberPrincipal principal) {
        RoleChecker.requireFinancialSecretary(principal);

        boolean alreadyRecorded = paymentRepository.existsByChurchIdAndPaymentTypeAndPaymentDate(
                principal.getChurchId(), PaymentType.OFFERING, request.getServiceDate());
        if (alreadyRecorded) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An offering has already been recorded for this date. If you need to correct it, contact your administrator.");
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

        int monthsCovered = Math.max(1,
                request.getAmountPaid().divide(monthlyAmount, 0, RoundingMode.FLOOR).intValue());

        YearMonth startMonth = YearMonth.parse(request.getPaymentMonth());
        List<PaymentResponse> responses = new ArrayList<>();

        for (int i = 0; i < monthsCovered; i++) {
            String monthStr = startMonth.plusMonths(i).toString();

            boolean alreadyPaid = !paymentRepository.findByChurchIdAndMemberIdAndPaymentTypeAndPaymentMonth(
                    principal.getChurchId(), request.getMemberId(),
                    PaymentType.WELFARE, monthStr).isEmpty();

            if (!alreadyPaid) {
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

        for (Member member : defaulters) {
            notificationService.notifyMember(
                    member,
                    "Welfare Reminder",
                    "You have not paid your welfare for " + paymentMonth + ". Please pay as soon as possible."
            );
        }
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
