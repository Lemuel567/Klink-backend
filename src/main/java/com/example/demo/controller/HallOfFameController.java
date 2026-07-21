package com.example.demo.controller;

import com.example.demo.dto.request.CreateHallOfFameRequest;
import com.example.demo.dto.request.UpdateHallOfFameRequest;
import com.example.demo.dto.response.HallOfFameResponse;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.HallOfFameService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hall-of-fame")
@RequiredArgsConstructor
public class HallOfFameController {

    private final HallOfFameService hallOfFameService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HallOfFameResponse> createEntry(
            @RequestPart("title") String title,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart(value = "memberId", required = false) String memberId,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();

        CreateHallOfFameRequest request = new CreateHallOfFameRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setMemberId(parseMemberId(memberId));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(hallOfFameService.createEntry(request, photo, principal));
    }

    @GetMapping
    public ResponseEntity<Page<HallOfFameResponse>> getAllEntries(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(hallOfFameService.getAllEntries(principal, pageable));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HallOfFameResponse> updateEntry(
            @PathVariable UUID id,
            @RequestPart(value = "title", required = false) String title,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart(value = "memberId", required = false) String memberId,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();

        UpdateHallOfFameRequest request = new UpdateHallOfFameRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setMemberId(parseMemberId(memberId));

        return ResponseEntity.ok(hallOfFameService.updateEntry(id, request, photo, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(
            @PathVariable UUID id,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        hallOfFameService.deleteEntry(id, principal);
        return ResponseEntity.noContent().build();
    }

    // A malformed UUID from the client must be a 400, not an unhandled 500.
    private UUID parseMemberId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "memberId must be a valid UUID");
        }
    }
}
