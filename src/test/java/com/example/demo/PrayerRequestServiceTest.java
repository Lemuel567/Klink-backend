package com.example.demo;
//Tests for church member prayer request submission and event publishing

import com.example.demo.dto.request.CreatePrayerRequestRequest;
import com.example.demo.dto.request.RespondPrayerRequestRequest;
import com.example.demo.dto.response.PrayerRequestResponse;
import com.example.demo.event.PrayerRequestCreatedEvent;
import com.example.demo.model.*;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.PrayerRequestRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.PrayerRequestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrayerRequestServiceTest {

    @Mock PrayerRequestRepository prayerRequestRepository;
    @Mock MemberRepository memberRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks PrayerRequestService prayerRequestService;

    // ── helpers ───────────────────────────────────────────────────────────────

    private MemberPrincipal principal(UUID churchId, Role role) {
        Church church = Church.builder().id(churchId).churchName("Test Church").build();
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

    private PrayerRequest request(UUID churchId, UUID authorId, PrayerVisibility visibility) {
        Church church = Church.builder().id(churchId).build();
        return PrayerRequest.builder()
                .id(UUID.randomUUID())
                .church(church)
                .memberId(authorId)
                .title("Please pray")
                .content("Pray for my family")
                .visibility(visibility)
                .status(PrayerStatus.OPEN)
                .build();
    }

    // ── create ──────────────────────────────────────────────────────────────

    @Test
    void anyMember_canSubmit_andLeadersAreNotified() {
        UUID churchId = UUID.randomUUID();
        MemberPrincipal member = principal(churchId, Role.MEMBER);

        CreatePrayerRequestRequest req = new CreatePrayerRequestRequest();
        ReflectionTestUtils.setField(req, "title", "Healing");
        ReflectionTestUtils.setField(req, "content", "Pray for healing");
        ReflectionTestUtils.setField(req, "visibility", PrayerVisibility.PUBLIC);

        when(prayerRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PrayerRequestResponse response = prayerRequestService.createPrayerRequest(req, member);

        assertThat(response.getTitle()).isEqualTo("Healing");
        assertThat(response.getStatus()).isEqualTo(PrayerStatus.OPEN);
        verify(prayerRequestRepository).save(any(PrayerRequest.class));

        // a PrayerRequestCreatedEvent must be published so Pastor/Elder get notified after commit
        verify(eventPublisher).publishEvent(any(PrayerRequestCreatedEvent.class));
    }

    // ── visibility ────────────────────────────────────────────────────────────

    @Test
    void privateRequest_isHiddenFromOtherMembers() {
        UUID churchId = UUID.randomUUID();
        MemberPrincipal viewer = principal(churchId, Role.MEMBER);
        UUID otherAuthor = UUID.randomUUID();
        PrayerRequest pr = request(churchId, otherAuthor, PrayerVisibility.PRIVATE);

        when(prayerRequestRepository.findByChurchIdAndIdAndDeletedAtIsNull(churchId, pr.getId()))
                .thenReturn(Optional.of(pr));

        assertThatThrownBy(() -> prayerRequestService.getPrayerRequest(pr.getId(), viewer))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void privateRequest_isVisibleToPastor() {
        UUID churchId = UUID.randomUUID();
        MemberPrincipal pastor = principal(churchId, Role.PASTOR);
        UUID author = UUID.randomUUID();
        PrayerRequest pr = request(churchId, author, PrayerVisibility.PRIVATE);

        when(prayerRequestRepository.findByChurchIdAndIdAndDeletedAtIsNull(churchId, pr.getId()))
                .thenReturn(Optional.of(pr));
        when(memberRepository.findByChurchIdAndId(churchId, author))
                .thenReturn(Optional.of(Member.builder().id(author).fullName("Author").build()));

        PrayerRequestResponse response = prayerRequestService.getPrayerRequest(pr.getId(), pastor);

        assertThat(response.getVisibility()).isEqualTo(PrayerVisibility.PRIVATE);
        assertThat(response.getMemberName()).isEqualTo("Author");
    }

    // ── respond ─────────────────────────────────────────────────────────────

    @Test
    void member_cannotRespond() {
        UUID churchId = UUID.randomUUID();
        MemberPrincipal member = principal(churchId, Role.MEMBER);

        RespondPrayerRequestRequest req = new RespondPrayerRequestRequest();
        ReflectionTestUtils.setField(req, "response", "Praying for you");

        assertThatThrownBy(() -> prayerRequestService.respond(UUID.randomUUID(), req, member))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void elder_respond_marksAnswered() {
        UUID churchId = UUID.randomUUID();
        MemberPrincipal elder = principal(churchId, Role.ELDER);
        UUID author = UUID.randomUUID();
        PrayerRequest pr = request(churchId, author, PrayerVisibility.PUBLIC);

        when(prayerRequestRepository.findByChurchIdAndIdAndDeletedAtIsNull(churchId, pr.getId()))
                .thenReturn(Optional.of(pr));
        when(prayerRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(memberRepository.findByChurchIdAndId(churchId, author))
                .thenReturn(Optional.empty());

        RespondPrayerRequestRequest req = new RespondPrayerRequestRequest();
        ReflectionTestUtils.setField(req, "response", "We are praying with you");

        PrayerRequestResponse response = prayerRequestService.respond(pr.getId(), req, elder);

        assertThat(response.getStatus()).isEqualTo(PrayerStatus.ANSWERED);
        assertThat(pr.getAnsweredBy()).isEqualTo(elder.getMemberId());
        assertThat(pr.getAnsweredAt()).isNotNull();
        assertThat(pr.getLeaderResponse()).isEqualTo("We are praying with you");
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void nonAuthorMember_cannotDelete() {
        UUID churchId = UUID.randomUUID();
        MemberPrincipal member = principal(churchId, Role.MEMBER);
        PrayerRequest pr = request(churchId, UUID.randomUUID(), PrayerVisibility.PUBLIC);

        when(prayerRequestRepository.findByChurchIdAndIdAndDeletedAtIsNull(churchId, pr.getId()))
                .thenReturn(Optional.of(pr));

        assertThatThrownBy(() -> prayerRequestService.deletePrayerRequest(pr.getId(), member))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void author_canSoftDelete() {
        UUID churchId = UUID.randomUUID();
        MemberPrincipal member = principal(churchId, Role.MEMBER);
        PrayerRequest pr = request(churchId, member.getMemberId(), PrayerVisibility.PUBLIC);

        when(prayerRequestRepository.findByChurchIdAndIdAndDeletedAtIsNull(churchId, pr.getId()))
                .thenReturn(Optional.of(pr));
        when(prayerRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        prayerRequestService.deletePrayerRequest(pr.getId(), member);

        assertThat(pr.getDeletedAt()).isNotNull();
        verify(prayerRequestRepository).save(pr);
    }
}
