package com.example.demo.controller;

import com.example.demo.dto.response.GalleryResponse;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.GalleryService;
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

@RestController
@RequestMapping("/api/v1/gallery")
@RequiredArgsConstructor
public class GalleryController {

    private final GalleryService galleryService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GalleryResponse> uploadPhoto(
            @RequestPart("photo") MultipartFile photo,
            @RequestPart(value = "caption", required = false) String caption,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(galleryService.uploadPhoto(photo, caption, principal));
    }

    @GetMapping
    public ResponseEntity<Page<GalleryResponse>> getAllPhotos(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(galleryService.getAllPhotos(principal, pageable));
    }
}
