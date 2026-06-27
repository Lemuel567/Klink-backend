package com.example.demo.controller;

import com.example.demo.dto.response.ChurchFileResponse;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.ChurchFileService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class ChurchFileController {

    private final ChurchFileService churchFileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChurchFileResponse> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart("title") String title,
            @RequestPart("category") String category,
            @RequestPart(value = "language", required = false) String language,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(churchFileService.uploadFile(file, title, category, language, principal));
    }

    @GetMapping
    public ResponseEntity<Page<ChurchFileResponse>> getFiles(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String language,
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(churchFileService.getFiles(category, language, principal, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable UUID id,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        churchFileService.deleteFile(id, principal);
        return ResponseEntity.noContent().build();
    }
}
