package com.example.demo.service;

import com.example.demo.dto.request.GenerateSermonNotesRequest;
import com.example.demo.dto.request.UploadSermonRequest;
import com.example.demo.dto.response.GeneratedNotesResponse;
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
    private final GeminiService geminiService;

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

    /**
     * Expands a manager's brief sermon notes into a detailed, warm summary via
     * Gemini. Pure generate-and-return — nothing is persisted here. The manager
     * reviews/edits the draft in the compose form and it's saved only when they
     * submit the sermon (or update it), same as anything else they type.
     */
    @Transactional(readOnly = true)
    public GeneratedNotesResponse generateNotes(GenerateSermonNotesRequest request, MemberPrincipal principal) {
        Role role = principal.getRole();
        if (role != Role.PASTOR && role != Role.ELDER && role != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a Pastor, Elder, or Manager can generate sermon notes");
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are helping a church prepare detailed sermon notes for members who could not ")
              .append("attend the service. Using ONLY the information given below, write a detailed, warm, ")
              .append("well-organized explanation of what the sermon covered and its key takeaways — as if ")
              .append("summarising for someone who wasn't there. Structure it in clear paragraphs (no markdown, ")
              .append("no headings, no bullet points — plain prose paragraphs only). Stay strictly grounded in ")
              .append("what is provided: do not invent specific scripture quotations, stories, or claims that ")
              .append("are not implied by the notes below. If the notes are brief, expand thoughtfully on the ")
              .append("theme and structure they suggest rather than fabricating new content. Aim for 3-4 ")
              .append("paragraphs.\n\n");
        prompt.append("Sermon title: ").append(request.getTitle()).append("\n");
        if (request.getPreacher() != null && !request.getPreacher().isBlank()) {
            prompt.append("Preacher: ").append(request.getPreacher()).append("\n");
        }
        if (request.getScripture() != null && !request.getScripture().isBlank()) {
            prompt.append("Scripture reading: ").append(request.getScripture()).append("\n");
        }
        if (request.getMemoryVerse() != null && !request.getMemoryVerse().isBlank()) {
            prompt.append("Memory verse: ").append(request.getMemoryVerse()).append("\n");
        }
        prompt.append("Preacher/manager's brief notes:\n").append(request.getNotes());

        String generated = geminiService.generateText(prompt.toString());
        return new GeneratedNotesResponse(generated);
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
