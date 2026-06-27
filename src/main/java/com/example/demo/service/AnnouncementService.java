package com.example.demo.service;

import com.example.demo.dto.request.PostAnnouncementRequest;
import com.example.demo.dto.response.AnnouncementResponse;
import com.example.demo.event.NotificationEvent;
import com.example.demo.model.Announcement;
import com.example.demo.repository.AnnouncementRepository;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final SupabaseStorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    public AnnouncementResponse postAnnouncement(PostAnnouncementRequest request,
                                                  MultipartFile flyer,
                                                  MemberPrincipal principal) {
        RoleChecker.requireManager(principal);

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        if (request.getBody() == null || request.getBody().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body is required");
        }

        String flyerUrl = null;
        if (flyer != null && !flyer.isEmpty()) {
            flyerUrl = storageService.uploadImage(flyer,
                    "announcements/" + principal.getChurchId());
        }

        Announcement announcement = Announcement.builder()
                .church(principal.getMember().getChurch())
                .title(request.getTitle())
                .body(request.getBody())
                .flyerUrl(flyerUrl)
                .postedBy(principal.getMemberId())
                .build();

        AnnouncementResponse response = AnnouncementResponse.from(announcementRepository.save(announcement));

        eventPublisher.publishEvent(new NotificationEvent(
                this,
                principal.getChurchId(),
                "New Announcement: " + request.getTitle(),
                request.getBody().length() > 100
                        ? request.getBody().substring(0, 100) + "..."
                        : request.getBody()
        ));

        return response;
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> getAllAnnouncements(MemberPrincipal principal, Pageable pageable) {
        return announcementRepository
                .findByChurchIdOrderByCreatedAtDesc(principal.getChurchId(), pageable)
                .map(AnnouncementResponse::from);
    }

    public void deleteAnnouncement(UUID announcementId, MemberPrincipal principal) {
        RoleChecker.requireManager(principal);
        Announcement announcement = announcementRepository.findByChurchIdAndId(principal.getChurchId(), announcementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));
        if (announcement.getFlyerUrl() != null) {
            storageService.deleteFile(announcement.getFlyerUrl());
        }
        announcementRepository.delete(announcement);
    }
}
