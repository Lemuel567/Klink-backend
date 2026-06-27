package com.example.demo.service;

import com.example.demo.dto.request.*;
import com.example.demo.dto.response.*;
import com.example.demo.model.*;
import com.example.demo.repository.*;
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
import java.time.LocalDateTime;
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

    public GroupResponse createGroup(CreateGroupRequest request, MemberPrincipal principal) {
        RoleChecker.requirePastorOrElder(principal);

        Member groupAdmin = null;
        Member groupFinSec = null;

        if (request.getGroupAdminId() != null) {
            groupAdmin = memberRepository.findByChurchIdAndId(principal.getChurchId(), request.getGroupAdminId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group admin member not found"));
        }

        if (request.getGroupFinSecId() != null) {
            groupFinSec = memberRepository.findByChurchIdAndId(principal.getChurchId(), request.getGroupFinSecId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group financial secretary not found"));
        }

        if (groupAdmin != null && groupFinSec != null &&
                groupAdmin.getId().equals(groupFinSec.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Group Admin and Group Financial Secretary must be different people");
        }

        GroupStatus status = (groupAdmin != null) ? GroupStatus.ACTIVE : GroupStatus.DRAFT;

        Group group = Group.builder()
                .church(principal.getMember().getChurch())
                .groupName(request.getGroupName())
                .description(request.getDescription())
                .duesAmount(request.getDuesAmount())
                .groupAdmin(groupAdmin)
                .groupFinSec(groupFinSec)
                .status(status)
                .createdBy(principal.getMemberId())
                .build();

        return GroupResponse.from(groupRepository.save(group));
    }

    @Transactional(readOnly = true)
    public Page<GroupResponse> getAllGroups(MemberPrincipal principal, Pageable pageable) {
        // Pastor and Elder see ALL groups in their church for management oversight.
        // Everyone else (Group Admin, FinSec, Member) sees only groups they belong to.
        if (RoleChecker.isPastorOrElder(principal)) {
            return groupRepository.findByChurchId(principal.getChurchId(), pageable)
                    .map(GroupResponse::from);
        }
        return groupRepository.findGroupsByMembership(
                        principal.getChurchId(), principal.getMemberId(), pageable)
                .map(GroupResponse::from);
    }

    public GroupResponse addMember(UUID groupId, AddGroupMemberRequest request, MemberPrincipal principal) {
        RoleChecker.requirePastorOrElder(principal);

        Group group = getGroupForChurch(groupId, principal.getChurchId());

        Member member = memberRepository.findByChurchIdAndId(principal.getChurchId(), request.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        if (groupMemberRepository.existsByGroupIdAndMemberId(groupId, request.getMemberId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Member is already in this group");
        }

        GroupMember groupMember = GroupMember.builder()
                .church(principal.getMember().getChurch())
                .group(group)
                .member(member)
                .joinedAt(LocalDateTime.now())
                .build();

        groupMemberRepository.save(groupMember);
        return GroupResponse.from(group);
    }

    public GroupMessageResponse postMessage(UUID groupId, PostGroupMessageRequest request, MemberPrincipal principal) {
        Group group = getGroupForChurch(groupId, principal.getChurchId());

        if (group.getGroupAdmin() == null ||
                !group.getGroupAdmin().getId().equals(principal.getMemberId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the Group Admin can post messages in this group");
        }

        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content cannot be empty");
        }

        GroupMessage message = GroupMessage.builder()
                .church(principal.getMember().getChurch())
                .group(group)
                .content(request.getContent())
                .postedBy(principal.getMemberId())
                .build();

        return GroupMessageResponse.from(groupMessageRepository.save(message));
    }

    @Transactional(readOnly = true)
    public Page<GroupMessageResponse> getMessages(UUID groupId, MemberPrincipal principal, Pageable pageable) {
        Group group = getGroupForChurch(groupId, principal.getChurchId());
        boolean isGroupAdmin = group.getGroupAdmin() != null &&
                group.getGroupAdmin().getId().equals(principal.getMemberId());
        boolean isMember = groupMemberRepository.existsByGroupIdAndMemberId(groupId, principal.getMemberId());

        if (!isGroupAdmin && !isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }

        return groupMessageRepository
                .findByChurchIdAndGroupIdOrderByCreatedAtAsc(principal.getChurchId(), groupId, pageable)
                .map(GroupMessageResponse::from);
    }

    public PaymentResponse recordDues(UUID groupId, RecordDuesRequest request, MemberPrincipal principal) {
        Group group = getGroupForChurch(groupId, principal.getChurchId());

        if (group.getGroupFinSec() == null ||
                !group.getGroupFinSec().getId().equals(principal.getMemberId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the Group Financial Secretary can record dues payments");
        }

        Member member = memberRepository.findByChurchIdAndId(principal.getChurchId(), request.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

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

        boolean isGroupAdmin = group.getGroupAdmin() != null &&
                group.getGroupAdmin().getId().equals(principal.getMemberId());
        boolean isGroupFinSec = group.getGroupFinSec() != null &&
                group.getGroupFinSec().getId().equals(principal.getMemberId());

        if (!isGroupAdmin && !isGroupFinSec) {
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

        boolean isGroupFinSec = group.getGroupFinSec() != null &&
                group.getGroupFinSec().getId().equals(principal.getMemberId());

        if (!isGroupFinSec) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the Group Financial Secretary can generate dues");
        }

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

    private Group getGroupForChurch(UUID groupId, UUID churchId) {
        return groupRepository.findByChurchIdAndId(churchId, groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

}
