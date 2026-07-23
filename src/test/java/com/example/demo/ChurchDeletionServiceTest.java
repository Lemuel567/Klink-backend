package com.example.demo;

import com.example.demo.model.Church;
import com.example.demo.repository.*;
import com.example.demo.service.ChurchDeletionService;
import com.example.demo.service.SupabaseStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Regression guard for the purge's FK ordering. Group dues are `payments` rows
 * holding a group_id FK — if groups are deleted before payments, Postgres
 * rejects the delete, the whole purge transaction rolls back, and the church
 * (with all its member PII) survives past the promised 30-day window forever.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChurchDeletionServiceTest {

    @Mock ChurchRepository churchRepository;
    @Mock MemberRepository memberRepository;
    @Mock AttendanceRepository attendanceRepository;
    @Mock AttendanceSessionRepository attendanceSessionRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock PledgePaymentRepository pledgePaymentRepository;
    @Mock PledgeRepository pledgeRepository;
    @Mock GroupMessageRepository groupMessageRepository;
    @Mock GroupMemberRepository groupMemberRepository;
    @Mock GroupRepository groupRepository;
    @Mock AnnouncementRepository announcementRepository;
    @Mock AnnouncementReadRepository announcementReadRepository;
    @Mock EventRepository eventRepository;
    @Mock SermonRepository sermonRepository;
    @Mock DevotionalRepository devotionalRepository;
    @Mock GalleryRepository galleryRepository;
    @Mock ChurchFileRepository churchFileRepository;
    @Mock PollVoteRepository pollVoteRepository;
    @Mock PollRepository pollRepository;
    @Mock HallOfFameRepository hallOfFameRepository;
    @Mock StorePaymentRepository storePaymentRepository;
    @Mock StoreItemRepository storeItemRepository;
    @Mock FacilityImageRepository facilityImageRepository;
    @Mock FacilityRepository facilityRepository;
    @Mock ProjectContributionRepository projectContributionRepository;
    @Mock ProjectImageRepository projectImageRepository;
    @Mock ProjectUpdateRepository projectUpdateRepository;
    @Mock ChurchProjectRepository churchProjectRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock VerificationTokenRepository verificationTokenRepository;
    @Mock PrayerRequestRepository prayerRequestRepository;
    @Mock PaystackTransactionRepository paystackTransactionRepository;
    @Mock GivingScheduleRepository givingScheduleRepository;
    @Mock SupabaseStorageService storageService;

    @InjectMocks ChurchDeletionService churchDeletionService;

    @Test
    void permanentlyDelete_removesPaymentsBeforeGroups_andCoversEveryChild() {
        UUID churchId = UUID.randomUUID();
        Church church = Church.builder().id(churchId).churchName("Purged Church").build();
        when(memberRepository.findByChurchId(churchId)).thenReturn(List.of());
        when(galleryRepository.findByChurchIdOrderByUploadedAtDesc(churchId)).thenReturn(List.of());
        when(churchFileRepository.findByChurchId(churchId)).thenReturn(List.of());
        when(sermonRepository.findByChurchIdOrderBySermonDateDesc(churchId)).thenReturn(List.of());
        when(facilityImageRepository.findByChurchId(churchId)).thenReturn(List.of());
        when(projectImageRepository.findByChurchId(churchId)).thenReturn(List.of());
        when(hallOfFameRepository.findByChurchIdOrderByCreatedAtDesc(churchId)).thenReturn(List.of());
        when(announcementRepository.findByChurchIdOrderByCreatedAtDesc(churchId)).thenReturn(List.of());
        when(groupRepository.findByChurchId(churchId)).thenReturn(List.of());
        when(storeItemRepository.findByChurchId(churchId)).thenReturn(List.of());

        churchDeletionService.permanentlyDelete(church);

        // THE ordering that matters: payments (FK → groups) strictly before groups
        InOrder order = inOrder(paymentRepository, groupMessageRepository, groupMemberRepository, groupRepository);
        order.verify(paymentRepository).deleteAllByChurchId(churchId);
        order.verify(groupMessageRepository).deleteAllByChurchId(churchId);
        order.verify(groupMemberRepository).deleteAllByChurchId(churchId);
        order.verify(groupRepository).deleteAllByChurchId(churchId);

        // members go last among church children, church record after them
        InOrder tail = inOrder(givingScheduleRepository, memberRepository, churchRepository);
        tail.verify(givingScheduleRepository).deleteAllByChurchId(churchId);
        tail.verify(memberRepository).deleteAllByChurchId(churchId);
        tail.verify(churchRepository).delete(church);

        // Every church-scoped table is purged — a new table missing here is a leak
        verify(paystackTransactionRepository).deleteAllByChurchId(churchId);
        verify(prayerRequestRepository).deleteAllByChurchId(churchId);
        verify(projectContributionRepository).deleteAllByChurchId(churchId);
        verify(churchProjectRepository).deleteAllByChurchId(churchId);
        verify(storePaymentRepository).deleteAllByChurchId(churchId);
        verify(pollVoteRepository).deleteAllByChurchId(churchId);
        verify(announcementReadRepository).deleteAllByChurchId(churchId);
        verify(pledgePaymentRepository).deleteAllByChurchId(churchId);
        verify(attendanceRepository).deleteAllByChurchId(churchId);
        verify(attendanceSessionRepository).deleteAllByChurchId(churchId);
        verify(givingScheduleRepository).deleteAllByChurchId(churchId);
    }
}
