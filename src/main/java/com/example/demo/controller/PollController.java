package com.example.demo.controller;

import com.example.demo.dto.request.CreatePollRequest;
import com.example.demo.dto.request.VoteRequest;
import com.example.demo.dto.response.PollResponse;
import com.example.demo.dto.response.PollResultsResponse;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.PollService;
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
@RequestMapping("/api/v1/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    @PostMapping
    public ResponseEntity<PollResponse> createPoll(
            @Valid @RequestBody CreatePollRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pollService.createPoll(request, principal));
    }

    @GetMapping
    public ResponseEntity<Page<PollResponse>> getAllPolls(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(pollService.getAllPolls(principal, pageable));
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<PollResponse> vote(
            @PathVariable UUID id,
            @Valid @RequestBody VoteRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(pollService.vote(id, request, principal));
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<PollResultsResponse> getResults(
            @PathVariable UUID id,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(pollService.getResults(id, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePoll(
            @PathVariable UUID id,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        pollService.deletePoll(id, principal);
        return ResponseEntity.noContent().build();
    }
}
