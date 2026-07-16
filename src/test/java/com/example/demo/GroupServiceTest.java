package com.example.demo;
//Tests for sel- contained mini church groups = roles , membership,posts, appointment and sues
import com.example.demo.dto.request.AddGroupMemberRequest;
import com.example.demo.dto.request.CreateGroupRequest;
import com.example.demo.dto.request.PostGroupMessageRequest;
import com.example.demo.dto.request.RecordDuesRequest;
import com.example.demo.dto.response.GroupMessageResponse;
import com.example.demo.dto.response.GroupResponse;
import com.example.demo.dto.response.PaymentResponse;
import com.example.demo.event.TargetedNotificationEvent;
import com.example.demo.model.*;
import com.example.demo.repository.GroupMemberRepository;
import com.example.demo.repository.GroupMessageRepository;
import com.example.demo.repository.GroupRepository;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.GroupService;
import com.example.demo.service.SupabaseStorageService;
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

/**
 * Unit tests for the self-contained mini-church group model.
 *
 * These tests verify the following core invariants:
 *  - Group roles are determined by group-specific foreign keys, not church-wide roles.
 *    A user with a regular MEMBER church role can still serve as a group's admin or finance secretary.
 *  - Only the group admin can manage membership, posts, and appointments.
 *  - Only the finance secretary can manage dues, and all group financial data is accessible
 *    exclusively through the admin/finance secretary authorization boundary.
 */
@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock GroupRepository groupRepository;
    @Mock GroupMemberRepository groupMemberRepository;
    @Mock GroupMessageRepository groupMessageRepository;
    @Mock MemberRepository memberRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock SupabaseStorageService supabaseStorageService;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks GroupService groupService;

    private final UUID churchId = UUID.randomUUID();

    // ── helpers ───────────────────────────────────────────────────────────────

    private MemberPrincipal principal(UUID memberId, Role role) {
        Church church = Church.builder().id(churchId).churchName("Test Church").build();
        Member member = Member.builder()
                .id(memberId)
                .church(church)
                .role(role)
                .status(MemberStatus.ACTIVE)
                .fullName("Test " + role)
                .hasSmartphone(true)
                .build();
        return new MemberPrincipal(member, churchId);
    }

    private Member member(UUID id, String name) {
        Church church = Church.builder().id(churchId).build();
        return Member.builder()
                .id(id).church(church).fullName(name)
                .role(Role.MEMBER).status(MemberStatus.ACTIVE).hasSmartphone(true)
                .build();
    }

    private Group group(UUID id, Member admin, Member finSec) {
        Church church = Church.builder().id(churchId).churchName("Test Church").build();
        return Group.builder()
                .id(id).church(church).groupName("Choir")
                .duesAmount(new BigDecimal("50.00"))
                .groupAdmin(admin).groupFinSec(finSec)
                .status(admin != null ? GroupStatus.ACTIVE : GroupStatus.DRAFT)
                .build();
    }

    private CreateGroupRequest createRequest(String name, UUID adminId) {
        CreateGroupRequest req = new CreateGroupRequest();
        ReflectionTestUtils.setField(req, "groupName", name);
        ReflectionTestUtils.setField(req, "groupAdminId", adminId);
        return req;
    }

    private AddGroupMemberRequest memberRequest(UUID memberId) {
        AddGroupMemberRequest req = new AddGroupMemberRequest();
        ReflectionTestUtils.setField(req, "memberId", memberId);
        return req;
    }

    private void assertForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertStatus(call, HttpStatus.FORBIDDEN);
    }

    private void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, HttpStatus status) {
        assertThatThrownBy(call)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(status));
    }

    // ── create (church-role gated) ─────────────────────────────────────────────

    @Test
    void createGroup_byRegularMember_isForbidden() {
        MemberPrincipal member = principal(UUID.randomUUID(), Role.MEMBER);
        assertForbidden(() -> groupService.createGroup(createRequest("Choir", null), member));
    }

    @Test
    void createGroup_withoutAdmin_staysDraft_andNooneIsAutoAdded() {
        MemberPrincipal pastor = principal(UUID.randomUUID(), Role.PASTOR);
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> {
            Group g = inv.getArgument(0);
            if (g.getId() == null) ReflectionTestUtils.setField(g, "id", UUID.randomUUID());
            return g;
        });
        when(groupMemberRepository.countByGroupId(any())).thenReturn(0L);

        GroupResponse resp = groupService.createGroup(createRequest("Choir", null), pastor);

        assertThat(resp.getStatus()).isEqualTo(GroupStatus.DRAFT);
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void createGroup_withAdmin_becomesActive_andAdminIsAutoAdded() {
        MemberPrincipal pastor = principal(UUID.randomUUID(), Role.PASTOR);
        UUID adminId = UUID.randomUUID();
        when(memberRepository.findByChurchIdAndId(churchId, adminId))
                .thenReturn(Optional.of(member(adminId, "Group Admin")));
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> {
            Group g = inv.getArgument(0);
            if (g.getId() == null) ReflectionTestUtils.setField(g, "id", UUID.randomUUID());
            return g;
        });
        when(groupMemberRepository.existsByGroupIdAndMemberId(any(), eq(adminId))).thenReturn(false);
        when(groupMemberRepository.countByGroupId(any())).thenReturn(1L);

        GroupResponse resp = groupService.createGroup(createRequest("Choir", adminId), pastor);

        assertThat(resp.getStatus()).isEqualTo(GroupStatus.ACTIVE);
        verify(groupMemberRepository).save(any(GroupMember.class));
    }

    // ── membership is admin-only (FK-gated, not church-role) ───────────────────

    @Test
    void addMember_byNonAdmin_isForbidden() {
        UUID groupId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        MemberPrincipal notAdmin = principal(UUID.randomUUID(), Role.MEMBER);
        when(groupRepository.findByChurchIdAndId(churchId, groupId))
                .thenReturn(Optional.of(group(groupId, member(adminId, "Admin"), null)));

        assertForbidden(() -> groupService.addMember(groupId, memberRequest(UUID.randomUUID()), notAdmin));
    }

    @Test
    void addMember_byGroupAdminWhoIsARegularMember_succeeds() {
        UUID groupId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID newMemberId = UUID.randomUUID();
        // admin holds church role MEMBER — permission comes from the group FK, not the role
        MemberPrincipal admin = principal(adminId, Role.MEMBER);
        when(groupRepository.findByChurchIdAndId(churchId, groupId))
                .thenReturn(Optional.of(group(groupId, member(adminId, "Admin"), null)));
        when(memberRepository.findByChurchIdAndId(churchId, newMemberId))
                .thenReturn(Optional.of(member(newMemberId, "New Member")));
        when(groupMemberRepository.existsByGroupIdAndMemberId(groupId, newMemberId)).thenReturn(false);
        when(groupMemberRepository.countByGroupId(groupId)).thenReturn(2L);

        groupService.addMember(groupId, memberRequest(newMemberId), admin);

        verify(groupMemberRepository).save(any(GroupMember.class));
    }

    @Test
    void removeMember_cannotRemoveTheAdmin() {
        UUID groupId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        MemberPrincipal admin = principal(adminId, Role.MEMBER);
        when(groupRepository.findByChurchIdAndId(churchId, groupId))
                .thenReturn(Optional.of(group(groupId, member(adminId, "Admin"), null)));

        assertStatus(() -> groupService.removeMember(groupId, adminId, admin), HttpStatus.BAD_REQUEST);
        verify(groupMemberRepository, never()).deleteByGroupIdAndMemberId(any(), any());
    }

    // ── fin-sec appointment (admin-only, must already be a member, not the admin) ─

    @Test
    void assignFinSec_byNonAdmin_isForbidden() {
        UUID groupId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        MemberPrincipal notAdmin = principal(UUID.randomUUID(), Role.MEMBER);
        when(groupRepository.findByChurchIdAndId(churchId, groupId))
                .thenReturn(Optional.of(group(groupId, member(adminId, "Admin"), null)));

        assertForbidden(() -> groupService.assignFinSec(groupId, memberRequest(UUID.randomUUID()), notAdmin));
    }

    @Test
    void assignFinSec_targetMustAlreadyBeAGroupMember() {
        UUID groupId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID finSecId = UUID.randomUUID();
        MemberPrincipal admin = principal(adminId, Role.MEMBER);
        when(groupRepository.findByChurchIdAndId(churchId, groupId))
                .thenReturn(Optional.of(group(groupId, member(adminId, "Admin"), null)));
        when(memberRepository.findByChurchIdAndId(churchId, finSecId))
                .thenReturn(Optional.of(member(finSecId, "Fin Sec")));
        when(groupMemberRepository.existsByGroupIdAndMemberId(groupId, finSecId)).thenReturn(false);

        assertStatus(() -> groupService.assignFinSec(groupId, memberRequest(finSecId), admin),
                HttpStatus.BAD_REQUEST);
    }

    @Test
    void assignFinSec_cannotBeTheSamePersonAsAdmin() {
        UUID groupId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        MemberPrincipal admin = principal(adminId, Role.MEMBER);
        when(groupRepository.findByChurchIdAndId(churchId, groupId))
                .thenReturn(Optional.of(group(groupId, member(adminId, "Admin"), null)));
        when(memberRepository.findByChurchIdAndId(churchId, adminId))
                .thenReturn(Optional.of(member(adminId, "Admin")));

        assertStatus(() -> groupService.assignFinSec(groupId, memberRequest(adminId), admin),
                HttpStatus.BAD_REQUEST);
    }

    // ── posts are admin-only and notify the rest of the group ──────────────────

    @Test
    void postMessage_byNonAdmin_isForbidden() {
        UUID groupId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        MemberPrincipal notAdmin = principal(UUID.randomUUID(), Role.MEMBER);
        when(groupRepository.findByChurchIdAndId(churchId, groupId))
                .thenReturn(Optional.of(group(groupId, member(adminId, "Admin"), null)));

        PostGroupMessageRequest req = new PostGroupMessageRequest();
        ReflectionTestUtils.setField(req, "content", "Rehearsal at 4pm");

        assertForbidden(() -> groupService.postMessage(groupId, req, notAdmin));
    }

    @Test
    void postMessage_byAdmin_notifiesOtherMembersAfterCommit() {
        UUID groupId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        MemberPrincipal admin = principal(adminId, Role.MEMBER);
        Group g = group(groupId, member(adminId, "Admin"), null);
        when(groupRepository.findByChurchIdAndId(churchId, groupId)).thenReturn(Optional.of(g));
        when(groupMessageRepository.save(any(GroupMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(groupMemberRepository.findByGroupId(groupId)).thenReturn(List.of(
                GroupMember.builder().group(g).member(member(adminId, "Admin")).build(),
                GroupMember.builder().group(g).member(member(otherId, "Other")).build()
        ));

        PostGroupMessageRequest req = new PostGroupMessageRequest();
        ReflectionTestUtils.setField(req, "content", "Rehearsal at 4pm");

        GroupMessageResponse resp = groupService.postMessage(groupId, req, admin);

        assertThat(resp.getContent()).isEqualTo("Rehearsal at 4pm");
        verify(eventPublisher).publishEvent(any(TargetedNotificationEvent.class));
    }

    // ── dues are fin-sec-only; group money stays behind the admin/fin-sec gate ─

    @Test
    void recordDues_byNonFinSec_isForbidden() {
        UUID groupId = UUID.randomUUID();
        UUID finSecId = UUID.randomUUID();
        MemberPrincipal notFinSec = principal(UUID.randomUUID(), Role.MEMBER);
        when(groupRepository.findByChurchIdAndId(churchId, groupId))
                .thenReturn(Optional.of(group(groupId, null, member(finSecId, "Fin Sec"))));

        RecordDuesRequest req = duesRequest(UUID.randomUUID());
        assertForbidden(() -> groupService.recordDues(groupId, req, notFinSec));
    }

    @Test
    void recordDues_byGroupFinSecWhoIsARegularMember_records() {
        UUID groupId = UUID.randomUUID();
        UUID finSecId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        // fin-sec holds church role MEMBER — permission comes from the group FK
        MemberPrincipal finSec = principal(finSecId, Role.MEMBER);
        when(groupRepository.findByChurchIdAndId(churchId, groupId))
                .thenReturn(Optional.of(group(groupId, null, member(finSecId, "Fin Sec"))));
        when(memberRepository.findByChurchIdAndId(churchId, payerId))
                .thenReturn(Optional.of(member(payerId, "Payer")));
        when(groupMemberRepository.existsByGroupIdAndMemberId(groupId, payerId)).thenReturn(true);
        when(paymentRepository.findByGroupIdAndMemberIdAndPaymentMonth(groupId, payerId, "2026-07"))
                .thenReturn(List.of());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse resp = groupService.recordDues(groupId, duesRequest(payerId), finSec);

        assertThat(resp.getPaymentType()).isEqualTo(PaymentType.DUES);
        assertThat(resp.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void financeSummary_isHiddenFromRegularMembers() {
        UUID groupId = UUID.randomUUID();
        MemberPrincipal member = principal(UUID.randomUUID(), Role.MEMBER);
        when(groupRepository.findByChurchIdAndId(churchId, groupId))
                .thenReturn(Optional.of(group(groupId,
                        member(UUID.randomUUID(), "Admin"), member(UUID.randomUUID(), "Fin Sec"))));

        assertForbidden(() -> groupService.getFinanceSummary(groupId, member));
        verify(paymentRepository, never()).sumConfirmedByGroupId(any());
    }

    private RecordDuesRequest duesRequest(UUID payerId) {
        RecordDuesRequest req = new RecordDuesRequest();
        ReflectionTestUtils.setField(req, "memberId", payerId);
        ReflectionTestUtils.setField(req, "amount", new BigDecimal("50.00"));
        ReflectionTestUtils.setField(req, "paymentMonth", "2026-07");
        ReflectionTestUtils.setField(req, "paymentDate", LocalDate.now());
        return req;
    }
}
