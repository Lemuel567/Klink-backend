package com.example.demo.controller;

import com.example.demo.dto.request.PostAnnouncementRequest;
import com.example.demo.dto.response.AnnouncementResponse;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.AnnouncementService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnnouncementResponse> postAnnouncement(
            @RequestPart("title") String title,
            @RequestPart("body") String body,
            @RequestPart(value = "flyer", required = false) MultipartFile flyer,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        PostAnnouncementRequest request = new PostAnnouncementRequest();
        request.setTitle(title);
        request.setBody(body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(announcementService.postAnnouncement(request, flyer, principal));
    }

    @GetMapping
    public ResponseEntity<Page<AnnouncementResponse>> getAllAnnouncements(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(announcementService.getAllAnnouncements(principal, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnnouncement(
            @PathVariable UUID id,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        announcementService.deleteAnnouncement(id, principal);
        return ResponseEntity.noContent().build();
    }
}
