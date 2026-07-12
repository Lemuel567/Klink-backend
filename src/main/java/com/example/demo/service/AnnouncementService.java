package com.example.demo.service;

import com.example.demo.dto.request.PatchAnnouncementRequest;
import com.example.demo.dto.request.PostAnnouncementRequest;
import com.example.demo.dto.response.AnnouncementRecipientResponse;
import com.example.demo.dto.response.AnnouncementResponse;
import com.example.demo.dto.response.GroupSummaryResponse;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.security.RoleChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final MemberRepository memberRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final SupabaseStorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    public AnnouncementResponse postAnnouncement(PostAnnouncementRequest request,
                                                  MultipartFile flyer,
                                                  MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        if (request.getBody() == null || request.getBody().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body is required");
        }

        AnnouncementTargetType targetType = request.getTargetType() != null
                ? request.getTargetType()
                : AnnouncementTargetType.ALL;

        validateTargeting(targetType, request.getTargetRoles(), request.getTargetGroupIds(), request.getTargetMemberIds());

        boolean isTargeted = targetType != AnnouncementTargetType.ALL;

        String flyerUrl = null;
        if (flyer != null && !flyer.isEmpty()) {
            flyerUrl = storageService.uploadImage(flyer, "announcements/" + principal.getChurchId());
        }

        // Step 1 — resolve recipients before saving so we can set recipientCount
        List<Member> recipients = resolveRecipients(
                principal.getChurchId(), targetType,
                request.getTargetRoles(), request.getTargetGroupIds(), request.getTargetMemberIds()
        );

        // Step 2 — save announcement
        Announcement announcement = Announcement.builder()
                .church(principal.getMember().getChurch())
                .title(request.getTitle())
                .body(request.getBody())
                .flyerUrl(flyerUrl)
                .postedBy(principal.getMemberId())
                .targetType(targetType)
                .targetRoles(request.getTargetRoles())
                .targetGroupIds(request.getTargetGroupIds())
                .targetMemberIds(request.getTargetMemberIds())
                .isTargeted(isTargeted)
                .recipientCount(recipients.size())
                .build();

        announcement = announcementRepository.save(announcement);

        // Step 3 — notify recipients async AFTER_COMMIT so a rollback never sends
        // phantom notifications and large recipient lists never block the request thread
        String notifTitle = "New Announcement: " + request.getTitle();
        String notifBody = request.getBody().length() > 100
                ? request.getBody().substring(0, 100) + "..."
                : request.getBody();

        if (targetType == AnnouncementTargetType.ALL) {
            eventPublisher.publishEvent(new com.example.demo.event.NotificationEvent(
                    this, principal.getChurchId(), notifTitle, notifBody
            ));
        } else {
            eventPublisher.publishEvent(new com.example.demo.event.TargetedNotificationEvent(
                    this, principal.getChurchId(),
                    recipients.stream().map(Member::getId).toList(),
                    notifTitle, notifBody
            ));
        }

        log.info("AUDIT announcement_sent actor={} church={} targetType={} recipients={}",
                principal.getMemberId(), principal.getChurchId(), targetType, recipients.size());

        return AnnouncementResponse.from(announcement);
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> getAllAnnouncements(MemberPrincipal principal, Pageable pageable) {
        // Privileged roles see everything (management view). Regular members must not
        // read announcements targeted at other roles/groups/members — filter to theirs.
        Role role = principal.getRole();
        boolean isPrivileged = role == Role.PASTOR || role == Role.ELDER || role == Role.MANAGER;
        if (!isPrivileged) {
            return getAnnouncementsForMember(principal, pageable);
        }
        return announcementRepository
                .findByChurchIdOrderByCreatedAtDesc(principal.getChurchId(), pageable)
                .map(AnnouncementResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> getAnnouncementsForMember(MemberPrincipal principal, Pageable pageable) {
        UUID churchId = principal.getChurchId();
        UUID memberId = principal.getMemberId();
        Role memberRole = principal.getRole();

        // Fetch member's group IDs
        Set<UUID> memberGroupIds = new HashSet<>(
                groupMemberRepository.findGroupIdsByChurchIdAndMemberId(churchId, memberId)
        );

        // Fetch recent 200 announcements and filter in memory (church-scale volumes)
        List<Announcement> all = announcementRepository.findTop200ByChurchIdOrderByCreatedAtDesc(churchId);

        List<AnnouncementResponse> filtered = all.stream()
                .filter(a -> isAnnouncementForMember(a, memberId, memberRole, memberGroupIds))
                .map(AnnouncementResponse::from)
                .collect(Collectors.toList());

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<AnnouncementResponse> page = start >= filtered.size() ? List.of() : filtered.subList(start, end);

        return new PageImpl<>(page, pageable, filtered.size());
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementRecipientResponse> getRecipients(UUID announcementId,
                                                              MemberPrincipal principal,
                                                              Pageable pageable) {
        RoleChecker.requirePastorElderOrManager(principal);

        Announcement announcement = announcementRepository
                .findByChurchIdAndId(principal.getChurchId(), announcementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));

        List<Member> recipients = resolveRecipients(
                principal.getChurchId(),
                safeType(announcement.getTargetType()),
                announcement.getTargetRoles(),
                announcement.getTargetGroupIds(),
                announcement.getTargetMemberIds()
        );

        List<AnnouncementRecipientResponse> responses = recipients.stream()
                .map(AnnouncementRecipientResponse::from)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), responses.size());
        List<AnnouncementRecipientResponse> page = start >= responses.size() ? List.of() : responses.subList(start, end);

        return new PageImpl<>(page, pageable, responses.size());
    }

    @Transactional(readOnly = true)
    public List<GroupSummaryResponse> getGroupsForTargeting(MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);

        List<Group> groups = groupRepository.findByChurchIdAndStatus(principal.getChurchId(), GroupStatus.ACTIVE);
        if (groups.isEmpty()) return List.of();

        List<UUID> groupIds = groups.stream().map(Group::getId).collect(Collectors.toList());
        List<Object[]> counts = groupMemberRepository.countMembersByGroupIds(groupIds);

        Map<UUID, Long> countMap = new HashMap<>();
        for (Object[] row : counts) {
            countMap.put((UUID) row[0], (Long) row[1]);
        }

        return groups.stream()
                .map(g -> GroupSummaryResponse.of(g.getId(), g.getGroupName(), countMap.getOrDefault(g.getId(), 0L)))
                .collect(Collectors.toList());
    }

    public AnnouncementResponse patchAnnouncement(UUID announcementId,
                                                   PatchAnnouncementRequest request,
                                                   MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);

        Announcement announcement = announcementRepository
                .findByChurchIdAndId(principal.getChurchId(), announcementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            announcement.setTitle(request.getTitle());
        }
        if (request.getBody() != null && !request.getBody().isBlank()) {
            announcement.setBody(request.getBody());
        }

        return AnnouncementResponse.from(announcementRepository.save(announcement));
    }

    public void deleteAnnouncement(UUID announcementId, MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);

        Announcement announcement = announcementRepository
                .findByChurchIdAndId(principal.getChurchId(), announcementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));

        if (announcement.getFlyerUrl() != null) {
            storageService.deleteFile(announcement.getFlyerUrl());
        }
        announcementRepository.delete(announcement);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateTargeting(AnnouncementTargetType type,
                                   List<Role> roles,
                                   List<UUID> groupIds,
                                   List<UUID> memberIds) {
        switch (type) {
            case ROLES -> {
                if (roles == null || roles.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "targetRoles must not be empty when targetType is ROLES");
                }
            }
            case GROUPS -> {
                if (groupIds == null || groupIds.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "targetGroupIds must not be empty when targetType is GROUPS");
                }
            }
            case MEMBERS -> {
                if (memberIds == null || memberIds.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "targetMemberIds must not be empty when targetType is MEMBERS");
                }
            }
            case CUSTOM -> {
                if ((roles == null || roles.isEmpty()) && (groupIds == null || groupIds.isEmpty())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "At least one of targetRoles or targetGroupIds must be provided when targetType is CUSTOM");
                }
            }
            case ALL -> { /* no validation needed */ }
        }
    }

    private List<Member> resolveRecipients(UUID churchId,
                                           AnnouncementTargetType type,
                                           List<Role> roles,
                                           List<UUID> groupIds,
                                           List<UUID> memberIds) {
        return switch (type) {
            case ALL -> memberRepository.findByChurchIdAndStatus(churchId, MemberStatus.ACTIVE);
            case ROLES -> roles == null || roles.isEmpty()
                    ? List.of()
                    : memberRepository.findByChurchIdAndRoleIn(churchId, roles).stream()
                        .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                        .collect(Collectors.toList());
            case GROUPS -> groupIds == null || groupIds.isEmpty()
                    ? List.of()
                    : groupMemberRepository.findMembersByGroupIdsInChurch(churchId, groupIds).stream()
                        .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                        .collect(Collectors.toList());
            case MEMBERS -> memberIds == null || memberIds.isEmpty()
                    ? List.of()
                    : memberRepository.findByChurchIdAndIdIn(churchId, memberIds).stream()
                        .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                        .collect(Collectors.toList());
            case CUSTOM -> {
                Set<UUID> seen = new HashSet<>();
                List<Member> result = new ArrayList<>();
                if (roles != null && !roles.isEmpty()) {
                    memberRepository.findByChurchIdAndRoleIn(churchId, roles).stream()
                            .filter(m -> m.getStatus() == MemberStatus.ACTIVE && seen.add(m.getId()))
                            .forEach(result::add);
                }
                if (groupIds != null && !groupIds.isEmpty()) {
                    groupMemberRepository.findMembersByGroupIdsInChurch(churchId, groupIds).stream()
                            .filter(m -> m.getStatus() == MemberStatus.ACTIVE && seen.add(m.getId()))
                            .forEach(result::add);
                }
                yield result;
            }
        };
    }

    private boolean isAnnouncementForMember(Announcement a, UUID memberId, Role role, Set<UUID> memberGroupIds) {
        AnnouncementTargetType type = a.getTargetType() != null ? a.getTargetType() : AnnouncementTargetType.ALL;
        return switch (type) {
            case ALL -> true;
            case ROLES -> a.getTargetRoles() != null && a.getTargetRoles().contains(role);
            case GROUPS -> a.getTargetGroupIds() != null
                    && !Collections.disjoint(new HashSet<>(a.getTargetGroupIds()), memberGroupIds);
            case MEMBERS -> a.getTargetMemberIds() != null && a.getTargetMemberIds().contains(memberId);
            case CUSTOM -> (a.getTargetRoles() != null && a.getTargetRoles().contains(role))
                    || (a.getTargetGroupIds() != null
                        && !Collections.disjoint(new HashSet<>(a.getTargetGroupIds()), memberGroupIds));
        };
    }

    private AnnouncementTargetType safeType(AnnouncementTargetType t) {
        return t != null ? t : AnnouncementTargetType.ALL;
    }
}
