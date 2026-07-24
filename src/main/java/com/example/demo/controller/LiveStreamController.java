package com.example.demo.controller;

import com.example.demo.dto.request.StartLiveStreamRequest;
import com.example.demo.dto.response.LiveStreamResponse;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.LiveStreamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/live-streams")
@RequiredArgsConstructor
public class LiveStreamController {

    private final LiveStreamService liveStreamService;

    /** Pastor / Elder / Manager — go live and notify the church. */
    @PostMapping
    public ResponseEntity<LiveStreamResponse> startStream(
            @Valid @RequestBody StartLiveStreamRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(liveStreamService.startStream(request, principal));
    }

    /** Pastor / Elder / Manager — end the broadcast. */
    @PutMapping("/{id}/end")
    public ResponseEntity<LiveStreamResponse> endStream(
            @PathVariable UUID id,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(liveStreamService.endStream(id, principal));
    }

    /**
     * All roles — the church's current broadcast.
     * Returns 204 NO CONTENT when nothing is live (so the client gets a clean
     * "not live" signal instead of an empty 200 body).
     */
    @GetMapping("/live")
    public ResponseEntity<LiveStreamResponse> getCurrentLive(Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        LiveStreamResponse live = liveStreamService.getCurrentLive(principal);
        return live == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(live);
    }

    /** All roles — past broadcasts (still watchable as YouTube recordings). */
    @GetMapping
    public ResponseEntity<Page<LiveStreamResponse>> getStreams(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(liveStreamService.getStreams(principal, pageable));
    }

    /** Pastor / Elder / Manager — remove a stream from the church's list. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStream(
            @PathVariable UUID id,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        liveStreamService.deleteStream(id, principal);
        return ResponseEntity.noContent().build();
    }
}
