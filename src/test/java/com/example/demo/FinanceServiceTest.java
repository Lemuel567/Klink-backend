package com.example.demo;
//To manage the finace sessions
import com.example.demo.dto.request.RecordOfferingRequest;
import com.example.demo.dto.request.RecordWelfareRequest;
import com.example.demo.dto.response.PaymentResponse;
import com.example.demo.model.*;
import com.example.demo.repository.ChurchRepository;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.FinanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock MemberRepository memberRepository;
    @Mock ChurchRepository churchRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks FinanceService financeService;

    // ── helpers ───────────────────────────────────────────────────────────────

    private MemberPrincipal principal(Role role) {
        UUID churchId = UUID.randomUUID();
        Church church = Church.builder().id(churchId).churchName("Test Church")
                .welfareAmount(new BigDecimal("20.00")).build();
        Member member = Member.builder()
                .id(UUID.randomUUID())
                .church(church)
                .role(role)
                .status(MemberStatus.ACTIVE)
                .fullName("Test " + role)
                .hasSmartphone(true)
                .build();
        return new MemberPrincipal(member, churchId);
    }

    private RecordWelfareRequest welfareRequest(UUID memberId, String amount, String month) {
        RecordWelfareRequest req = new RecordWelfareRequest();
        ReflectionTestUtils.setField(req, "memberId", memberId);
        ReflectionTestUtils.setField(req, "amountPaid", new BigDecimal(amount));
        ReflectionTestUtils.setField(req, "paymentMonth", month);
        ReflectionTestUtils.setField(req, "paymentDate", LocalDate.now());
        return req;
    }

    private void stubMemberAndChurch(MemberPrincipal p, UUID memberId) {
        Member member = Member.builder()
                .id(memberId).church(p.getMember().getChurch())
                .fullName("Payer").role(Role.MEMBER).status(MemberStatus.ACTIVE)
                .hasSmartphone(true).build();
        when(memberRepository.findByChurchIdAndId(p.getChurchId(), memberId))
                .thenReturn(Optional.of(member));
        when(churchRepository.findById(p.getChurchId()))
                .thenReturn(Optional.of(p.getMember().getChurch()));
    }

    // ── welfare accounting ────────────────────────────────────────────────────

    @Test
    void welfare_exactMultiple_recordsOneMonthPerMultiple() {
        MemberPrincipal p = principal(Role.FINANCIAL_SECRETARY);
        UUID memberId = UUID.randomUUID();
        stubMemberAndChurch(p, memberId);
        when(paymentRepository.findByChurchIdAndMemberIdAndPaymentTypeAndPaymentMonth(
                any(), any(), eq(PaymentType.WELFARE), any())).thenReturn(List.of());
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 60 = 3 × 20 → exactly 3 months recorded
        List<PaymentResponse> result = financeService.recordWelfare(
                welfareRequest(memberId, "60.00", "2026-07"), p);

        assertThat(result).hasSize(3);
        verify(paymentRepository, times(3)).save(any(Payment.class));
    }

    @Test
    void welfare_underpayment_isRejected_neverBuysAFullMonth() {
        MemberPrincipal p = principal(Role.FINANCIAL_SECRETARY);
        UUID memberId = UUID.randomUUID();
        stubMemberAndChurch(p, memberId);

        // 10 < monthly 20 — must be rejected, not silently recorded as a full month
        assertThatThrownBy(() -> financeService.recordWelfare(
                welfareRequest(memberId, "10.00", "2026-07"), p))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void welfare_nonMultiple_isRejected_noMoneyLost() {
        MemberPrincipal p = principal(Role.FINANCIAL_SECRETARY);
        UUID memberId = UUID.randomUUID();
        stubMemberAndChurch(p, memberId);

        // 50 = 2 × 20 + 10 remainder — remainder must not vanish; reject
        assertThatThrownBy(() -> financeService.recordWelfare(
                welfareRequest(memberId, "50.00", "2026-07"), p))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void welfare_nonFinancialSecretary_isForbidden() {
        MemberPrincipal p = principal(Role.MANAGER);

        assertThatThrownBy(() -> financeService.recordWelfare(
                welfareRequest(UUID.randomUUID(), "20.00", "2026-07"), p))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ── offering ──────────────────────────────────────────────────────────────

    @Test
    void offering_duplicateDate_returnsConflict() {
        MemberPrincipal p = principal(Role.FINANCIAL_SECRETARY);
        RecordOfferingRequest req = new RecordOfferingRequest();
        ReflectionTestUtils.setField(req, "serviceDate", LocalDate.now());
        ReflectionTestUtils.setField(req, "amount", new BigDecimal("500.00"));

        when(paymentRepository.existsByChurchIdAndPaymentTypeAndPaymentDate(
                eq(p.getChurchId()), eq(PaymentType.OFFERING), any())).thenReturn(true);

        assertThatThrownBy(() -> financeService.recordOffering(req, p))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    // ── welfare reminder off the request thread ───────────────────────────────

    @Test
    void welfareReminder_publishesEvent_insteadOfBlockingRequestThread() {
        MemberPrincipal p = principal(Role.FINANCIAL_SECRETARY);
        Member defaulter = Member.builder()
                .id(UUID.randomUUID()).church(p.getMember().getChurch())
                .fullName("Defaulter").role(Role.MEMBER).status(MemberStatus.ACTIVE)
                .hasSmartphone(true).build();
        when(memberRepository.findWelfareDefaulters(
                eq(p.getChurchId()), eq("2026-07"), eq(MemberStatus.ACTIVE), eq(PaymentType.WELFARE)))
                .thenReturn(List.of(defaulter));

        financeService.sendManualWelfareReminder("2026-07", p);

        verify(eventPublisher).publishEvent(any(com.example.demo.event.TargetedNotificationEvent.class));
    }
}
