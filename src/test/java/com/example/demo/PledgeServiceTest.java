package com.example.demo;

import com.example.demo.dto.request.RecordPledgePaymentRequest;
import com.example.demo.dto.response.PledgePaymentResponse;
import com.example.demo.model.*;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.PledgePaymentRepository;
import com.example.demo.repository.PledgeRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.PledgeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PledgeServiceTest {

    @Mock PledgeRepository pledgeRepository;
    @Mock PledgePaymentRepository pledgePaymentRepository;
    @Mock MemberRepository memberRepository;

    @InjectMocks PledgeService pledgeService;

    private MemberPrincipal principal(Role role) {
        UUID churchId = UUID.randomUUID();
        Church church = Church.builder().id(churchId).churchName("Test Church").build();
        Member member = Member.builder()
                .id(UUID.randomUUID())
                .church(church)
                .role(role)
                .status(MemberStatus.ACTIVE)
                .fullName("Test " + role)
                .build();
        return new MemberPrincipal(member, churchId);
    }

    private Pledge pledge(UUID churchId, String amount, String paid, PledgeStatus status) {
        Member pledger = Member.builder().id(UUID.randomUUID()).fullName("Pledger").build();
        return Pledge.builder()
                .id(UUID.randomUUID())
                .church(Church.builder().id(churchId).build())
                .member(pledger)
                .amount(new BigDecimal(amount))
                .amountPaid(new BigDecimal(paid))
                .status(status)
                .build();
    }

    private RecordPledgePaymentRequest payment(String amount) {
        RecordPledgePaymentRequest request = new RecordPledgePaymentRequest();
        ReflectionTestUtils.setField(request, "amount", new BigDecimal(amount));
        ReflectionTestUtils.setField(request, "paymentDate", LocalDate.of(2026, 7, 23));
        return request;
    }

    @Test
    void payPledge_loadsRowLocked_andAccumulatesAmountPaid() {
        MemberPrincipal finSec = principal(Role.FINANCIAL_SECRETARY);
        Pledge pledge = pledge(finSec.getChurchId(), "100.00", "60.00", PledgeStatus.PARTIALLY_PAID);
        when(pledgeRepository.findByChurchIdAndIdForUpdate(finSec.getChurchId(), pledge.getId()))
                .thenReturn(Optional.of(pledge));

        PledgePaymentResponse response = pledgeService.payPledge(pledge.getId(), payment("15.00"), finSec);

        // Locked finder is the load path — the plain finder would reintroduce the lost update
        verify(pledgeRepository).findByChurchIdAndIdForUpdate(finSec.getChurchId(), pledge.getId());
        verify(pledgeRepository, never()).findByChurchIdAndId(any(), any());
        assertThat(pledge.getAmountPaid()).isEqualByComparingTo("75.00");
        assertThat(pledge.getStatus()).isEqualTo(PledgeStatus.PARTIALLY_PAID);
        assertThat(response.getAmount()).isEqualByComparingTo("15.00");
    }

    @Test
    void payPledge_reachingFullAmount_marksPaidAndSetsPaidAt() {
        MemberPrincipal finSec = principal(Role.FINANCIAL_SECRETARY);
        Pledge pledge = pledge(finSec.getChurchId(), "100.00", "60.00", PledgeStatus.PARTIALLY_PAID);
        when(pledgeRepository.findByChurchIdAndIdForUpdate(finSec.getChurchId(), pledge.getId()))
                .thenReturn(Optional.of(pledge));

        pledgeService.payPledge(pledge.getId(), payment("40.00"), finSec);

        assertThat(pledge.getAmountPaid()).isEqualByComparingTo("100.00");
        assertThat(pledge.getStatus()).isEqualTo(PledgeStatus.PAID);
        assertThat(pledge.getPaidAt()).isEqualTo(LocalDate.of(2026, 7, 23));
        verify(pledgePaymentRepository).save(any(PledgePayment.class));
        verify(pledgeRepository).save(pledge);
    }

    @Test
    void payPledge_alreadyPaid_returnsConflict() {
        MemberPrincipal finSec = principal(Role.FINANCIAL_SECRETARY);
        Pledge pledge = pledge(finSec.getChurchId(), "100.00", "100.00", PledgeStatus.PAID);
        when(pledgeRepository.findByChurchIdAndIdForUpdate(finSec.getChurchId(), pledge.getId()))
                .thenReturn(Optional.of(pledge));

        assertThatThrownBy(() -> pledgeService.payPledge(pledge.getId(), payment("10.00"), finSec))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(pledgePaymentRepository, never()).save(any());
    }

    @Test
    void payPledge_nonFinancialSecretary_isForbidden() {
        MemberPrincipal member = principal(Role.MEMBER);

        assertThatThrownBy(() -> pledgeService.payPledge(UUID.randomUUID(), payment("10.00"), member))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(pledgePaymentRepository);
    }
}
