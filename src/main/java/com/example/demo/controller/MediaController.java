package com.example.demo.controller;

import com.example.demo.dto.response.MediaUploadResponse;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.MediaUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaUploadService mediaUploadService;

    @PostMapping("/upload")
    public ResponseEntity<MediaUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false, defaultValue = "uploads") String folder,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(mediaUploadService.upload(file, folder, principal));
    }
}