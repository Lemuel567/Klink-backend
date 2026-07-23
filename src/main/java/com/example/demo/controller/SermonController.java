package com.example.demo.controller;

import com.example.demo.dto.request.GenerateSermonNotesRequest;
import com.example.demo.dto.request.UploadSermonRequest;
import com.example.demo.dto.response.GeneratedNotesResponse;
import com.example.demo.dto.response.SermonResponse;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.SermonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sermons")
@RequiredArgsConstructor
public class SermonController {

    private final SermonService sermonService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SermonResponse> uploadSermon(
            @RequestPart("preacher") String preacher,
            @RequestPart("title") String title,
            @RequestPart(value = "memoryVerse", required = false) String memoryVerse,
            @RequestPart(value = "scripture", required = false) String scripture,
            @RequestPart("sermonDate") String sermonDate,
            @RequestPart(value = "notes", required = false) String notes,
            @RequestPart(value = "audio", required = false) MultipartFile audio,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();

        UploadSermonRequest request = new UploadSermonRequest();
        request.setPreacher(preacher);
        request.setTitle(title);
        request.setMemoryVerse(memoryVerse);
        request.setScripture(scripture);
        try {
            request.setSermonDate(LocalDate.parse(sermonDate));
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sermonDate must be in YYYY-MM-DD format");
        }
        request.setNotes(notes);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sermonService.uploadSermon(request, audio, principal));
    }

    // AI-assisted drafting: expands the manager's brief notes into a detailed
    // summary. Nothing is persisted — the manager reviews/edits the draft
    // before it's saved via the normal create/update flow.
    @PostMapping("/generate-notes")
    public ResponseEntity<GeneratedNotesResponse> generateNotes(
            @Valid @RequestBody GenerateSermonNotesRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(sermonService.generateNotes(request, principal));
    }

    @GetMapping
    public ResponseEntity<Page<SermonResponse>> getAllSermons(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(sermonService.getAllSermons(principal, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SermonResponse> getSermon(
            @PathVariable UUID id,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(sermonService.getSermon(id, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSermon(
            @PathVariable UUID id,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        sermonService.deleteSermon(id, principal);
        return ResponseEntity.noContent().build();
    }
}
