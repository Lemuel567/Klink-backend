package com.example.demo.service;

import com.example.demo.dto.request.UploadSermonRequest;
import com.example.demo.dto.response.SermonResponse;
import com.example.demo.event.NotificationEvent;
import com.example.demo.model.Role;
import com.example.demo.model.Sermon;
import com.example.demo.repository.SermonRepository;
import com.example.demo.security.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SermonService {

    private final SermonRepository sermonRepository;
    private final SupabaseStorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    public SermonResponse uploadSermon(UploadSermonRequest request,
                                       MultipartFile audio,
                                       MemberPrincipal principal) {
        Role role = principal.getRole();
        if (role != Role.PASTOR && role != Role.ELDER && role != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a Pastor, Elder, or Manager can upload sermons");
        }

        if (request.getPreacher() == null || request.getPreacher().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preacher name is required");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        if (request.getSermonDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sermon date is required");
        }

        String audioUrl = null;
        if (audio != null && !audio.isEmpty()) {
            audioUrl = storageService.uploadAudio(audio, "sermons/" + principal.getChurchId());
        }

        Sermon sermon = Sermon.builder()
                .church(principal.getMember().getChurch())
                .preacher(request.getPreacher())
                .title(request.getTitle())
                .memoryVerse(request.getMemoryVerse())
                .scripture(request.getScripture())
                .sermonDate(request.getSermonDate())
                .audioUrl(audioUrl)
                .notes(request.getNotes())
                .postedBy(principal.getMemberId())
                .build();

        SermonResponse response = SermonResponse.from(sermonRepository.save(sermon));

        eventPublisher.publishEvent(new NotificationEvent(
                this,
                principal.getChurchId(),
                "New Sermon: " + request.getTitle(),
                "Preached by " + request.getPreacher() + ". Open the app to listen."
        ));

        return response;
    }

    @Transactional(readOnly = true)
    public Page<SermonResponse> getAllSermons(MemberPrincipal principal, Pageable pageable) {
        return sermonRepository
                .findByChurchIdOrderBySermonDateDesc(principal.getChurchId(), pageable)
                .map(SermonResponse::from);
    }

    @Transactional(readOnly = true)
    public SermonResponse getSermon(UUID sermonId, MemberPrincipal principal) {
        Sermon sermon = sermonRepository.findByChurchIdAndId(principal.getChurchId(), sermonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sermon not found"));
        return SermonResponse.from(sermon);
    }

    public void deleteSermon(UUID sermonId, MemberPrincipal principal) {
        Role role = principal.getRole();
        if (role != Role.PASTOR && role != Role.ELDER && role != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a Pastor, Elder, or Manager can delete sermons");
        }
        Sermon sermon = sermonRepository.findByChurchIdAndId(principal.getChurchId(), sermonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sermon not found"));
        if (sermon.getAudioUrl() != null) {
            storageService.deleteFile(sermon.getAudioUrl());
        }
        sermonRepository.delete(sermon);
    }
}
