package com.example.demo.service;

import com.example.demo.dto.request.CreatePrayerRequestRequest;
import com.example.demo.dto.request.RespondPrayerRequestRequest;
import com.example.demo.dto.response.PrayerRequestResponse;
import com.example.demo.event.PrayerRequestCreatedEvent;
import com.example.demo.model.Member;
import com.example.demo.model.PrayerRequest;
import com.example.demo.model.PrayerStatus;
import com.example.demo.model.PrayerVisibility;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.PrayerRequestRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.security.RoleChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PrayerRequestService {

    private final PrayerRequestRepository prayerRequestRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** Any authenticated member may submit a prayer request. */
    public PrayerRequestResponse createPrayerRequest(CreatePrayerRequestRequest request, MemberPrincipal principal) {
        PrayerRequest pr = PrayerRequest.builder()
                .church(principal.getMember().getChurch())
                .memberId(principal.getMemberId())
                .title(request.getTitle())
                .content(request.getContent())
                .visibility(request.getVisibility())
                .status(PrayerStatus.OPEN)
                .build();

        pr = prayerRequestRepository.save(pr);

        // Notify Pastor(s)/Elder(s) after commit — never all members.
        eventPublisher.publishEvent(new PrayerRequestCreatedEvent(
                this, principal.getChurchId(), principal.getMember().getFullName(), pr.getTitle()));

        return PrayerRequestResponse.from(pr, principal.getMember().getFullName());
    }

    @Transactional(readOnly = true)
    public Page<PrayerRequestResponse> getPrayerRequests(MemberPrincipal principal, Pageable pageable) {
        UUID churchId = principal.getChurchId();

        // Leaders see everything; regular members see PUBLIC + their own PRIVATE.
        Page<PrayerRequest> page = RoleChecker.isPastorOrElder(principal)
                ? prayerRequestRepository.findByChurchIdAndDeletedAtIsNullOrderByCreatedAtDesc(churchId, pageable)
                : prayerRequestRepository.findVisibleForMember(churchId, principal.getMemberId(), pageable);

        Map<UUID, String> names = resolveNames(churchId, page.getContent());
        return page.map(p -> PrayerRequestResponse.from(p, names.get(p.getMemberId())));
    }

    @Transactional(readOnly = true)
    public PrayerRequestResponse getPrayerRequest(UUID id, MemberPrincipal principal) {
        PrayerRequest pr = load(id, principal);

        // A PRIVATE request is only visible to a leader or its author.
        if (pr.getVisibility() == PrayerVisibility.PRIVATE
                && !RoleChecker.isPastorOrElder(principal)
                && !pr.getMemberId().equals(principal.getMemberId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prayer request not found");
        }

        return PrayerRequestResponse.from(pr, resolveName(principal.getChurchId(), pr.getMemberId()));
    }

    /** Only a Pastor or Elder may respond, which marks the request answered. */
    public PrayerRequestResponse respond(UUID id, RespondPrayerRequestRequest request, MemberPrincipal principal) {
        RoleChecker.requirePastorOrElder(principal);

        PrayerRequest pr = load(id, principal);
        pr.setLeaderResponse(request.getResponse());
        pr.setStatus(PrayerStatus.ANSWERED);
        pr.setAnsweredBy(principal.getMemberId());
        pr.setAnsweredAt(LocalDateTime.now());

        pr = prayerRequestRepository.save(pr);
        return PrayerRequestResponse.from(pr, resolveName(principal.getChurchId(), pr.getMemberId()));
    }

    /** The author or a Pastor/Elder may soft-delete a request. */
    public void deletePrayerRequest(UUID id, MemberPrincipal principal) {
        PrayerRequest pr = load(id, principal);

        boolean isAuthor = pr.getMemberId().equals(principal.getMemberId());
        if (!isAuthor && !RoleChecker.isPastorOrElder(principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot delete this prayer request");
        }

        pr.setDeletedAt(LocalDateTime.now());
        prayerRequestRepository.save(pr);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PrayerRequest load(UUID id, MemberPrincipal principal) {
        return prayerRequestRepository
                .findByChurchIdAndIdAndDeletedAtIsNull(principal.getChurchId(), id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prayer request not found"));
    }

    private Map<UUID, String> resolveNames(UUID churchId, List<PrayerRequest> requests) {
        List<UUID> ids = requests.stream()
                .map(PrayerRequest::getMemberId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) return Map.of();
        return memberRepository.findByChurchIdAndIdIn(churchId, ids).stream()
                .collect(Collectors.toMap(Member::getId, Member::getFullName, (a, b) -> a));
    }

    private String resolveName(UUID churchId, UUID memberId) {
        if (memberId == null) return null;
        return memberRepository.findByChurchIdAndId(churchId, memberId)
                .map(Member::getFullName)
                .orElse(null);
    }
}
