package com.example.demo.controller;

import com.example.demo.dto.request.UpdateChurchSettingsRequest;
import com.example.demo.dto.response.ChurchResponse;
import com.example.demo.dto.response.MessageResponse;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.ChurchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/church")
@RequiredArgsConstructor
public class ChurchController {

    private final ChurchService churchService;

    @GetMapping("/settings")
    public ResponseEntity<ChurchResponse> getChurchSettings(Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(churchService.getChurchSettings(principal));
    }

    @PutMapping("/settings")
    public ResponseEntity<ChurchResponse> updateChurchSettings(
            @RequestBody UpdateChurchSettingsRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(churchService.updateChurchSettings(request, principal));
    }

    @PostMapping("/photo")
    public ResponseEntity<String> uploadChurchPhoto(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(churchService.uploadChurchPhoto(file, principal));
    }

    @PostMapping("/regenerate-code")
    public ResponseEntity<MessageResponse> regenerateChurchCode(Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        String newCode = churchService.regenerateChurchCode(principal);
        return ResponseEntity.ok(new MessageResponse("Church code updated. New code: " + newCode));
    }

    @DeleteMapping
    public ResponseEntity<ChurchResponse> deleteChurch(Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(churchService.deleteChurch(principal));
    }

    @PostMapping("/restore")
    public ResponseEntity<ChurchResponse> restoreChurch(Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(churchService.restoreChurch(principal));
    }
}
