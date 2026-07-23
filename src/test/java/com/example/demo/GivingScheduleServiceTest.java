package com.example.demo;

import com.example.demo.dto.request.CreateGivingScheduleRequest;
import com.example.demo.dto.response.GivingScheduleResponse;
import com.example.demo.model.*;
import com.example.demo.repository.GivingScheduleRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.GivingScheduleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GivingScheduleServiceTest {

    @Mock GivingScheduleRepository repository;

    @InjectMocks GivingScheduleService service;

    private MemberPrincipal principal() {
        UUID churchId = UUID.randomUUID();
        Church church = Church.builder().id(churchId).churchName("Test Church").build();
        Member member = Member.builder()
                .id(UUID.randomUUID())
                .church(church)
                .role(Role.MEMBER)
                .status(MemberStatus.ACTIVE)
                .fullName("Member")
                .build();
        return new MemberPrincipal(member, churchId);
    }

    private CreateGivingScheduleRequest request(OnlinePaymentType type, String amount, int day) {
        CreateGivingScheduleRequest r = new CreateGivingScheduleRequest();
        r.setPaymentType(type);
        r.setAmount(new BigDecimal(amount));
        r.setDayOfMonth(day);
        return r;
    }

    @Test
    void create_projectContribution_isRejected() {
        assertThatThrownBy(() -> service.create(request(OnlinePaymentType.PROJECT_CONTRIBUTION, "50.00", 5), principal()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(repository);
    }

    @Test
    void create_savesActiveScheduleForTheCaller() {
        MemberPrincipal p = principal();
        when(repository.save(any(GivingSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        GivingScheduleResponse response = service.create(request(OnlinePaymentType.TITHE, "100.00", 15), p);

        ArgumentCaptor<GivingSchedule> saved = ArgumentCaptor.forClass(GivingSchedule.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getMember()).isSameAs(p.getMember());
        assertThat(saved.getValue().isActive()).isTrue();
        assertThat(response.getPaymentType()).isEqualTo(OnlinePaymentType.TITHE);
        assertThat(response.getAmount()).isEqualByComparingTo("100.00");
        assertThat(response.getDayOfMonth()).isEqualTo(15);
    }

    @Test
    void setActive_isScopedToOwnerAndChurch_404Otherwise() {
        MemberPrincipal p = principal();
        UUID scheduleId = UUID.randomUUID();
        // Another member's schedule (or another church's) never comes back from
        // the (churchId, memberId, id) finder — the service must 404, not toggle.
        when(repository.findByChurchIdAndMemberIdAndId(p.getChurchId(), p.getMemberId(), scheduleId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setActive(scheduleId, false, p))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(repository, never()).save(any());
    }

    @Test
    void setActive_togglesAndPersists() {
        MemberPrincipal p = principal();
        GivingSchedule schedule = GivingSchedule.builder()
                .id(UUID.randomUUID())
                .church(p.getMember().getChurch())
                .member(p.getMember())
                .paymentType(OnlinePaymentType.OFFERING)
                .amount(new BigDecimal("20.00"))
                .dayOfMonth(1)
                .active(true)
                .build();
        when(repository.findByChurchIdAndMemberIdAndId(p.getChurchId(), p.getMemberId(), schedule.getId()))
                .thenReturn(Optional.of(schedule));
        when(repository.save(any(GivingSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        GivingScheduleResponse response = service.setActive(schedule.getId(), false, p);

        assertThat(schedule.isActive()).isFalse();
        assertThat(response.isActive()).isFalse();
        verify(repository).save(schedule);
    }

    @Test
    void delete_removesOnlyAnOwnedSchedule() {
        MemberPrincipal p = principal();
        GivingSchedule schedule = GivingSchedule.builder()
                .id(UUID.randomUUID())
                .church(p.getMember().getChurch())
                .member(p.getMember())
                .paymentType(OnlinePaymentType.WELFARE)
                .amount(new BigDecimal("20.00"))
                .dayOfMonth(2)
                .build();
        when(repository.findByChurchIdAndMemberIdAndId(p.getChurchId(), p.getMemberId(), schedule.getId()))
                .thenReturn(Optional.of(schedule));

        service.delete(schedule.getId(), p);

        verify(repository).delete(schedule);
    }
}
