package com.example.demo.service;

import com.example.demo.model.Church;
import com.example.demo.model.Member;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChurchDeletionService {

    private final ChurchRepository churchRepository;
    private final MemberRepository memberRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final PaymentRepository paymentRepository;
    private final PledgePaymentRepository pledgePaymentRepository;
    private final PledgeRepository pledgeRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final AnnouncementRepository announcementRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final EventRepository eventRepository;
    private final SermonRepository sermonRepository;
    private final DevotionalRepository devotionalRepository;
    private final GalleryRepository galleryRepository;
    private final ChurchFileRepository churchFileRepository;
    private final PollVoteRepository pollVoteRepository;
    private final PollRepository pollRepository;
    private final HallOfFameRepository hallOfFameRepository;
    private final StorePaymentRepository storePaymentRepository;
    private final StoreItemRepository storeItemRepository;
    private final FacilityImageRepository facilityImageRepository;
    private final FacilityRepository facilityRepository;
    private final ProjectContributionRepository projectContributionRepository;
    private final ProjectImageRepository projectImageRepository;
    private final ProjectUpdateRepository projectUpdateRepository;
    private final ChurchProjectRepository churchProjectRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PrayerRequestRepository prayerRequestRepository;
    private final PaystackTransactionRepository paystackTransactionRepository;
    private final SupabaseStorageService storageService;

    /**
     * Permanently deletes a church and all associated data.
     * DB deletion is atomic. Storage file deletion is best-effort — failures are logged
     * but do not prevent the DB records from being removed.
     */
    @Transactional
    public void permanentlyDelete(Church church) {
        UUID churchId = church.getId();
        log.info("Beginning permanent deletion of church {} ({})", church.getChurchName(), churchId);

        // Collect storage file URLs before deleting the records that reference them
        List<String> fileUrls = collectFileUrls(churchId, church);

        // Collect member IDs and emails before deleting members
        // (RefreshToken and VerificationToken are keyed by memberId / email, not churchId)
        List<Member> members = memberRepository.findByChurchId(churchId);
        List<UUID> memberIds = members.stream().map(Member::getId).collect(Collectors.toList());
        List<String> memberEmails = members.stream()
                .map(Member::getEmail)
                .filter(e -> e != null && !e.isBlank())
                .collect(Collectors.toList());

        // Delete in FK dependency order — children before parents
        paystackTransactionRepository.deleteAllByChurchId(churchId);
        prayerRequestRepository.deleteAllByChurchId(churchId);
        projectContributionRepository.deleteAllByChurchId(churchId);
        projectImageRepository.deleteAllByChurchId(churchId);
        projectUpdateRepository.deleteAllByChurchId(churchId);
        churchProjectRepository.deleteAllByChurchId(churchId);
        facilityImageRepository.deleteAllByChurchId(churchId);
        facilityRepository.deleteAllByChurchId(churchId);
        storePaymentRepository.deleteAllByChurchId(churchId);
        storeItemRepository.deleteAllByChurchId(churchId);
        pollVoteRepository.deleteAllByChurchId(churchId);
        pollRepository.deleteAllByChurchId(churchId);
        hallOfFameRepository.deleteAllByChurchId(churchId);
        galleryRepository.deleteAllByChurchId(churchId);
        churchFileRepository.deleteAllByChurchId(churchId);
        devotionalRepository.deleteAllByChurchId(churchId);
        sermonRepository.deleteAllByChurchId(churchId);
        eventRepository.deleteAllByChurchId(churchId);
        announcementReadRepository.deleteAllByChurchId(churchId);
        announcementRepository.deleteAllByChurchId(churchId);
        groupMessageRepository.deleteAllByChurchId(churchId);
        groupMemberRepository.deleteAllByChurchId(churchId);
        groupRepository.deleteAllByChurchId(churchId);
        pledgePaymentRepository.deleteAllByChurchId(churchId);
        pledgeRepository.deleteAllByChurchId(churchId);
        paymentRepository.deleteAllByChurchId(churchId);
        attendanceRepository.deleteAllByChurchId(churchId);
        attendanceSessionRepository.deleteAllByChurchId(churchId);

        if (!memberIds.isEmpty()) {
            refreshTokenRepository.deleteByMemberIdIn(memberIds);
        }
        if (!memberEmails.isEmpty()) {
            verificationTokenRepository.deleteByEmailIn(memberEmails);
        }

        memberRepository.deleteAllByChurchId(churchId);
        churchRepository.delete(church);

        log.info("DB records deleted for church {}. Cleaning up {} storage files.", churchId, fileUrls.size());

        // Best-effort storage cleanup — errors are logged but don't affect DB consistency
        for (String url : fileUrls) {
            if (url != null && !url.isBlank()) {
                try {
                    storageService.deleteFile(url);
                } catch (Exception e) {
                    log.warn("Failed to delete storage file {} for church {}: {}", url, churchId, e.getMessage());
                }
            }
        }

        log.info("Permanent deletion completed for church {}", churchId);
    }

    private List<String> collectFileUrls(UUID churchId, Church church) {
        List<String> urls = new ArrayList<>();

        if (church.getPhotoUrl() != null) urls.add(church.getPhotoUrl());

        galleryRepository.findByChurchIdOrderByUploadedAtDesc(churchId)
                .forEach(g -> urls.add(g.getPhotoUrl()));

        churchFileRepository.findByChurchId(churchId)
                .forEach(f -> urls.add(f.getFileUrl()));

        sermonRepository.findByChurchIdOrderBySermonDateDesc(churchId)
                .stream().filter(s -> s.getAudioUrl() != null)
                .forEach(s -> urls.add(s.getAudioUrl()));

        facilityImageRepository.findByChurchId(churchId)
                .forEach(fi -> urls.add(fi.getImageUrl()));

        projectImageRepository.findByChurchId(churchId)
                .forEach(pi -> urls.add(pi.getImageUrl()));

        hallOfFameRepository.findByChurchIdOrderByCreatedAtDesc(churchId)
                .stream().filter(h -> h.getPhotoUrl() != null)
                .forEach(h -> urls.add(h.getPhotoUrl()));

        announcementRepository.findByChurchIdOrderByCreatedAtDesc(churchId)
                .stream().filter(a -> a.getFlyerUrl() != null)
                .forEach(a -> urls.add(a.getFlyerUrl()));

        // Previously missed: member profile photos, group photos and store item
        // photos were orphaned in storage after a permanent church deletion.
        memberRepository.findByChurchId(churchId)
                .stream().filter(m -> m.getPhotoUrl() != null)
                .forEach(m -> urls.add(m.getPhotoUrl()));

        groupRepository.findByChurchId(churchId)
                .stream().filter(g -> g.getPhotoUrl() != null)
                .forEach(g -> urls.add(g.getPhotoUrl()));

        storeItemRepository.findByChurchId(churchId).forEach(item -> {
            if (item.getPhotoUrl() != null) urls.add(item.getPhotoUrl());
            if (item.getPhotoUrls() != null) {
                item.getPhotoUrls().stream().filter(u -> u != null).forEach(urls::add);
            }
        });

        return urls;
    }
}
