package com.example.demo.controller;

import com.example.demo.dto.request.PatchAnnouncementRequest;
import com.example.demo.dto.request.PostAnnouncementRequest;
import com.example.demo.dto.response.AnnouncementRecipientResponse;
import com.example.demo.dto.response.AnnouncementResponse;
import com.example.demo.dto.response.GroupSummaryResponse;
import com.example.demo.model.AnnouncementTargetType;
import com.example.demo.model.Role;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.AnnouncementService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final ObjectMapper objectMapper;

    // POST /api/v1/announcements — multipart/form-data
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnnouncementResponse> postAnnouncement(
            @RequestPart("title") String title,
            @RequestPart("body") String body,
            @RequestPart(value = "targetType", required = false) String targetTypeRaw,
            @RequestPart(value = "targetRoles", required = false) String targetRolesJson,
            @RequestPart(value = "targetGroupIds", required = false) String targetGroupIdsJson,
            @RequestPart(value = "targetMemberIds", required = false) String targetMemberIdsJson,
            @RequestPart(value = "flyer", required = false) MultipartFile flyer,
            Authentication authentication) {

        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();

        PostAnnouncementRequest request = new PostAnnouncementRequest();
        request.setTitle(title);
        request.setBody(body);

        if (targetTypeRaw != null && !targetTypeRaw.isBlank()) {
            try {
                request.setTargetType(AnnouncementTargetType.valueOf(targetTypeRaw.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid targetType: " + targetTypeRaw);
            }
        }

        request.setTargetRoles(parseJson(targetRolesJson, new TypeReference<List<Role>>() {}));
        request.setTargetGroupIds(parseJson(targetGroupIdsJson, new TypeReference<List<UUID>>() {}));
        request.setTargetMemberIds(parseJson(targetMemberIdsJson, new TypeReference<List<UUID>>() {}));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(announcementService.postAnnouncement(request, flyer, principal));
    }

    // GET /api/v1/announcements — all announcements for the church (all roles)
    @GetMapping
    public ResponseEntity<Page<AnnouncementResponse>> getAllAnnouncements(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(announcementService.getAllAnnouncements(principal, pageable));
    }

    // GET /api/v1/announcements/my — Returns announcements visible to the authenticated member based on their audience and targeting.
    @GetMapping("/my")
    public ResponseEntity<Page<AnnouncementResponse>> getMyAnnouncements(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(announcementService.getAnnouncementsForMember(principal, pageable));
    }

    // POST /api/v1/announcements/{id}/read — mark one announcement as read for the caller
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id, Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        announcementService.markRead(id, principal);
        return ResponseEntity.noContent().build();
    }

    // POST /api/v1/announcements/read-all — mark every visible announcement as read
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        announcementService.markAllRead(principal);
        return ResponseEntity.noContent().build();
    }

    // GET /api/v1/announcements/groups — list of groups for target selector (privileged)
    @GetMapping("/groups")
    public ResponseEntity<List<GroupSummaryResponse>> getGroupsForTargeting(Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(announcementService.getGroupsForTargeting(principal));
    }

    // GET /api/v1/announcements/{id}/recipients — who received a specific announcement
    @GetMapping("/{id}/recipients")
    public ResponseEntity<Page<AnnouncementRecipientResponse>> getRecipients(
            @PathVariable UUID id,
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(announcementService.getRecipients(id, principal, pageable));
    }

    // PATCH /api/v1/announcements/{id} — update title/body only
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnnouncementResponse> patchAnnouncement(
            @PathVariable UUID id,
            @RequestBody PatchAnnouncementRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(announcementService.patchAnnouncement(id, request, principal));
    }

    // DELETE /api/v1/announcements/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnnouncement(
            @PathVariable UUID id,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        announcementService.deleteAnnouncement(id, principal);
        return ResponseEntity.noContent().build();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private <T> T parseJson(String json, TypeReference<T> type) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON in request part: " + e.getMessage());
        }
    }
}
