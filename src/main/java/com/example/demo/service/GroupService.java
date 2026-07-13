package com.example.demo.service;

import com.example.demo.dto.request.*;
import com.example.demo.dto.response.*;
import com.example.demo.event.TargetedNotificationEvent;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.security.RoleChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final MemberRepository memberRepository;
    private final PaymentRepository paymentRepository;
    private final SupabaseStorageService supabaseStorageService;
    private final ApplicationEventPublisher eventPublisher;

    // ── Group lifecycle ──────────────────────────────────────────────────────

    /**
     * Leadership (Pastor / Elder / Manager) creates a group and may appoint the
     * admin. The creator is NEVER auto-added as a member — a Manager only becomes
     * part of a group if the group admin later adds them.
     */
    public GroupResponse createGroup(CreateGroupRequest request, MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);

        Member groupAdmin = null;
        if (request.getGroupAdminId() != null) {
            groupAdmin = loadChurchMember(principal.getChurchId(), request.getGroupAdminId(), "Group admin member not found");
        }

        GroupStatus status = (groupAdmin != null) ? GroupStatus.ACTIVE : GroupStatus.DRAFT;

        Group group = Group.builder()
                .church(principal.getMember().getChurch())
                .groupName(request.getGroupName())
                .description(request.getDescription())
                .duesAmount(request.getDuesAmount())
                .groupAdmin(groupAdmin)
                .status(status)
                .createdBy(principal.getMemberId())
                .build();

        group = groupRepository.save(group);

        // The admin leads — and therefore belongs to — their own group.
        if (groupAdmin != null) {
            ensureMembership(group, groupAdmin);
        }

        return GroupResponse.from(group, groupMemberRepository.countByGroupId(group.getId()));
    }

    public GroupResponse updateGroup(UUID groupId, UpdateGroupRequest request, MemberPrincipal principal) {
        Group group = getGroupForChurch(groupId, principal.getChurchId());
        requireGroupAdminOrLeadership(group, principal);

        if (request.getGroupName() != null && !request.getGroupName().isBlank()) {
            group.setGroupName(request.getGroupName().trim());
        }
        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }
        if (request.getDuesAmount() != null) {
            group.setDuesAmount(request.getDuesAmount());
        }
        return GroupResponse.from(group, groupMemberRepository.countByGroupId(groupId));
    }

    public GroupResponse uploadPhoto(UUID groupId, MultipartFile file, MemberPrincipal principal) {
        Group group = getGroupForChurch(groupId, principal.getChurchId());
        requireGroupAdminOrLeadership(group, principal);

        String url = supabaseStorageService.uploadImage(file, "groups");
        group.setPhotoUrl(url);
        return GroupResponse.from(group, groupMemberRepository.countByGroupId(groupId));
    }

    @Transactional(readOnly = true)
    public Page<GroupResponse> getAllGroups(MemberPrincipal principal, Pageable pageable) {
        // Leadership (Pastor/Elder/Manager) sees ALL groups for oversight — a Manager must be able
        // to find and open a group they created even though they are not a member of it.
        // Everyone else (Group Admin, FinSec, Member) sees only groups they belong to.
        Page<Group> groups = isLeadership(principal)
                ? groupRepository.findByChurchId(principal.getChurchId(), pageable)
                : groupRepository.findGroupsByMembership(principal.getChurchId(), principal.getMemberId(), pageable);

        return groups.map(g -> GroupResponse.from(g, groupMemberRepository.countByGroupId(g.getId())));
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroup(UUID groupId, MemberPrincipal principal) {
        Group group = getGroupForChurch(groupId, principal.getChurchId());
        requireGroupAccess(group, principal);
        return GroupResponse.from(group, groupMemberRepository.countByGroupId(groupId));
    }

    // ── Appointments ─────────────────────────────────────────────────────────

    /** Leadership appoints (or changes) the group admin. */
    public GroupResponse assignAdmin(UUID groupId, AddGroupMemberRequest request, MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);
        Group group = getGroupForChurch(groupId, principal.getChurchId());

        Member admin = loadChurchMember(principal.getChurchId(), request.getMemberId(), "Member not found");

        group.setGroupAdmin(admin);
        group.setStatus(GroupStatus.ACTIVE);
        ensureMembership(group, admin);

        return GroupResponse.from(group, groupMemberRepository.countByGroupId(groupId));
    }

    /**
     * The group admin appoints a group member as the group's financial secretary —
     * the person who records and handles the group's (separate) money.
     */
    public GroupResponse assignFinSec(UUID groupId, AddGroupMemberRequest request, MemberPrincipal principal) {
        Group group = getGroupForChurch(groupId, principal.getChurchId());
        requireGroupAdmin(group, principal);

        Member finSec = loadChurchMember(principal.getChurchId(), request.getMemberId(), "Member not found");

        if (group.getGroupAdmin() != null && group.getGroupAdmin().getId().equals(finSec.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The group admin cannot also be the group financial secretary");
        }
        if (!groupMemberRepository.existsByGroupIdAndMemberId(groupId, finSec.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You can only appoint a member of this group as financial secretary. Add them first.");
        }

        group.setGroupFinSec(finSec);
        return GroupResponse.from(group, groupMemberRepository.countByGroupId(groupId));
    }

    // ── Membership (admin only) ──────────────────────────────────────────────

    public GroupResponse addMember(UUID groupId, AddGroupMemberRequest request, MemberPrincipal principal) {
        Group group = getGroupForChurch(groupId, principal.getChurchId());
        requireGroupAdmin(group, principal);

        Member member = loadChurchMember(principal.getChurchId(), request.getMemberId(), "Member not found");

        if (groupMemberRepository.existsByGroupIdAndMemberId(groupId, request.getMemberId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Member is already in this group");
        }

        ensureMembership(group, member);
        return GroupResponse.from(group, groupMemberRepository.countByGroupId(groupId));
    }

    public void removeMember(UUID groupId, UUID memberId, MemberPrincipal principal) {
        Group group = getGroupForChurch(groupId, principal.getChurchId());
        requireGroupAdmin(group, principal);

        if (group.getGroupAdmin() != null && group.getGroupAdmin().getId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Assign a new group admin before removing this member");
        }
        if (!groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member is not in this group");
        }

        // Removing the fin sec drops their group-money role too.
        if (group.getGroupFinSec() != null && group.getGroupFinSec().getId().equals(memberId)) {
            group.setGroupFinSec(null);
        }
        groupMemberRepository.deleteByGroupIdAndMemberId(groupId, memberId);
    }

    @Transactional(readOnly = true)
    public List<GroupMemberResponse> listMembers(UUID groupId, MemberPrincipal principal) {
        Group group = getGroupForChurch(groupId, principal.getChurchId());
        requireGroupAccess(group, principal);

        UUID adminId = group.getGroupAdmin() != null ? group.getGroupAdmin().getId() : null;
        UUID finSecId = group.getGroupFinSec() != null ? group.getGroupFinSec().getId() : null;

        return groupMemberRepository.findByGroupId(groupId).stream()
                .map(gm -> GroupMemberResponse.from(gm, adminId, finSecId))
                .toList();
    }

    // ── Group information / announcements (admin only) ───────────────────────

    public GroupMessageResponse postMessage(UUID groupId, PostGroupMessageRequest request, MemberPrincipal principal) {
        Group group = getGroupForChurch(groupId, principal.getChurchId());
        requireGroupAdmin(group, principal);

        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content cannot be empty");
        }

        GroupMessage message = GroupMessage.builder()
                .church(principal.getMember().getChurch())
                .group(group)
                .content(request.getContent())
                .postedBy(principal.getMemberId())
                .build();
        message = groupMessageRepository.save(message);

        // Notify every group member (except the poster) — like a church announcement,
        // scoped to this group. AFTER_COMMIT + async so the post never blocks on push/SMS.
        List<UUID> recipients = groupMemberRepository.findByGroupId(groupId).stream()
                .map(gm -> gm.getMember().getId())
                .filter(id -> !id.equals(principal.getMemberId()))
                .toList();
        if (!recipients.isEmpty()) {
            eventPublisher.publishEvent(new TargetedNotificationEvent(
                    this, principal.getChurchId(), recipients,
                    group.getGroupName(),
                    request.getContent().length() > 140
                            ? request.getContent().substring(0, 137) + "..."
                            : request.getContent()));
        }

        return GroupMessageResponse.from(message);
    }

    @Transactional(readOnly = true)
    public Page<GroupMessageResponse> getMessages(UUID groupId, MemberPrincipal principal, Pageable pageable) {
        Group group = getGroupForChurch(groupId, principal.getChurchId());
        requireGroupAccess(group, principal);

        return groupMessageRepository
                .findByChurchIdAndGroupIdOrderByCreatedAtAsc(principal.getChurchId(), groupId, pageable)
                .map(GroupMessageResponse::from);
    }

    // ── Group money (separate from church money) ─────────────────────────────

    public PaymentResponse recordDues(UUID groupId, RecordDuesRequest request, MemberPrincipal principal) {
        Group group = getGroupForChurch(groupId, principal.getChurchId());
        requireGroupFinSec(group, principal);

        Member member = loadChurchMember(principal.getChurchId(), request.getMemberId(), "Member not found");

        if (!groupMemberRepository.existsByGroupIdAndMemberId(groupId, request.getMemberId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member is not part of this group");
        }

        List<Payment> existing = paymentRepository.findByGroupIdAndMemberIdAndPaymentMonth(
                groupId, request.getMemberId(), request.getPaymentMonth());

        boolean alreadyConfirmed = existing.stream().anyMatch(p -> p.getStatus() == PaymentStatus.CONFIRMED);
        if (alreadyConfirmed) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Dues already recorded for this member for " + request.getPaymentMonth());
        }

        // If the monthly generation job already created a PENDING record, promote it.
        // Otherwise create a fresh CONFIRMED record (member joined after generation ran, etc.).
        Payment payment = existing.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .findFirst()
                .orElse(null);

        if (payment != null) {
            payment.setAmount(request.getAmount());
            payment.setPaymentDate(request.getPaymentDate());
            payment.setStatus(PaymentStatus.CONFIRMED);
            payment.setRecordedBy(principal.getMemberId());
        } else {
            payment = Payment.builder()
                    .church(principal.getMember().getChurch())
                    .member(member)
                    .group(group)
                    .paymentType(PaymentType.DUES)
                    .amount(request.getAmount())
                    .paymentMonth(request.getPaymentMonth())
                    .paymentDate(request.getPaymentDate())
                    .status(PaymentStatus.CONFIRMED)
                    .recordedBy(principal.getMemberId())
                    .build();
        }

        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public List<DuesStatusResponse> getDuesStatus(UUID groupId, String paymentMonth, MemberPrincipal principal) {
        Group group = getGroupForChurch(groupId, principal.getChurchId());

        if (!isGroupAdmin(group, principal) && !isGroupFinSec(group, principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the Group Admin or Group Financial Secretary can view dues status");
        }

        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        List<Payment> paidPayments = paymentRepository.findByGroupIdAndPaymentMonth(groupId, paymentMonth);

        return members.stream().map(gm -> {
            Member m = gm.getMember();
            Payment paid = paidPayments.stream()
                    .filter(p -> p.getMember() != null
                            && p.getMember().getId().equals(m.getId())
                            && p.getStatus() == PaymentStatus.CONFIRMED)
                    .findFirst().orElse(null);

            return DuesStatusResponse.builder()
                    .memberId(m.getId())
                    .memberName(m.getFullName())
                    .paid(paid != null)
                    .amountPaid(paid != null ? paid.getAmount() : null)
                    .build();
        }).toList();
    }

    public MessageResponse generateDues(UUID groupId, String paymentMonth, MemberPrincipal principal) {
        Group group = getGroupForChurch(groupId, principal.getChurchId());
        requireGroupFinSec(group, principal);

        if (group.getDuesAmount() == null || group.getDuesAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This group has no dues amount configured");
        }

        List<Payment> existing = paymentRepository.findByGroupIdAndPaymentMonth(groupId, paymentMonth);
        Set<UUID> alreadyCovered = existing.stream()
                .filter(p -> p.getMember() != null)
                .map(p -> p.getMember().getId())
                .collect(Collectors.toSet());

        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        int created = 0;

        for (GroupMember gm : members) {
            if (alreadyCovered.contains(gm.getMember().getId())) continue;

            Payment dues = Payment.builder()
                    .church(principal.getMember().getChurch())
                    .member(gm.getMember())
                    .group(group)
                    .paymentType(PaymentType.DUES)
                    .amount(group.getDuesAmount())
                    .paymentMonth(paymentMonth)
                    .status(PaymentStatus.PENDING)
                    .build();
            paymentRepository.save(dues);
            created++;
        }

        return new MessageResponse("Generated " + created + " pending dues record(s) for " + paymentMonth);
    }

    /**
     * Group finance summary — every figure is drawn only from this group's payments
     * (group_id), so the group's money is completely independent of church money.
     */
    @Transactional(readOnly = true)
    public GroupFinanceSummaryResponse getFinanceSummary(UUID groupId, MemberPrincipal principal) {
        Group group = getGroupForChurch(groupId, principal.getChurchId());
        if (!isGroupAdmin(group, principal) && !isGroupFinSec(group, principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the Group Admin or Group Financial Secretary can view group finances");
        }

        String month = YearMonth.now().toString();
        BigDecimal total = paymentRepository.sumConfirmedByGroupId(groupId);
        BigDecimal thisMonth = paymentRepository.sumConfirmedByGroupIdAndMonth(groupId, month);
        long paidThisMonth = paymentRepository.countByGroupIdAndPaymentMonthAndStatus(
                groupId, month, PaymentStatus.CONFIRMED);
        long memberCount = groupMemberRepository.countByGroupId(groupId);

        List<PaymentResponse> recent = paymentRepository.findTop10ByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(PaymentResponse::from)
                .toList();

        return GroupFinanceSummaryResponse.of(
                total, thisMonth, month, memberCount, paidThisMonth, group.getDuesAmount(), recent);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Group getGroupForChurch(UUID groupId, UUID churchId) {
        return groupRepository.findByChurchIdAndId(churchId, groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    private Member loadChurchMember(UUID churchId, UUID memberId, String notFoundMessage) {
        return memberRepository.findByChurchIdAndId(churchId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, notFoundMessage));
    }

    /** Adds the member to the group if they are not already in it (idempotent). */
    private void ensureMembership(Group group, Member member) {
        if (!groupMemberRepository.existsByGroupIdAndMemberId(group.getId(), member.getId())) {
            groupMemberRepository.save(GroupMember.builder()
                    .church(group.getChurch())
                    .group(group)
                    .member(member)
                    .joinedAt(LocalDateTime.now())
                    .build());
        }
    }

    private boolean isGroupAdmin(Group group, MemberPrincipal principal) {
        return group.getGroupAdmin() != null
                && group.getGroupAdmin().getId().equals(principal.getMemberId());
    }

    private boolean isGroupFinSec(Group group, MemberPrincipal principal) {
        return group.getGroupFinSec() != null
                && group.getGroupFinSec().getId().equals(principal.getMemberId());
    }

    private boolean isGroupMember(Group group, MemberPrincipal principal) {
        return groupMemberRepository.existsByGroupIdAndMemberId(group.getId(), principal.getMemberId());
    }

    /** Church leadership with oversight over any group: Pastor, Elder, Manager. */
    private boolean isLeadership(MemberPrincipal principal) {
        return RoleChecker.isPastorOrElder(principal) || principal.getRole() == Role.MANAGER;
    }

    private void requireGroupAdmin(Group group, MemberPrincipal principal) {
        if (!isGroupAdmin(group, principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the Group Admin can perform this action");
        }
    }

    private void requireGroupFinSec(Group group, MemberPrincipal principal) {
        if (!isGroupFinSec(group, principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the Group Financial Secretary can perform this action");
        }
    }

    /** Group admin, or church leadership (Pastor/Elder/Manager) for oversight. */
    private void requireGroupAdminOrLeadership(Group group, MemberPrincipal principal) {
        if (isGroupAdmin(group, principal) || isLeadership(principal)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only the Group Admin or church leadership can perform this action");
    }

    /** Anyone connected to the group: admin, fin sec, member, or church leadership. */
    private void requireGroupAccess(Group group, MemberPrincipal principal) {
        if (isGroupAdmin(group, principal) || isGroupFinSec(group, principal)
                || isGroupMember(group, principal) || isLeadership(principal)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
    }
}
