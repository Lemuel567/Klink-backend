package com.example.demo.controller;

import com.example.demo.dto.request.*;
import com.example.demo.dto.response.*;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.createGroup(request, principal));
    }

    @GetMapping
    public ResponseEntity<Page<GroupResponse>> getAllGroups(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(groupService.getAllGroups(principal, pageable));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<GroupResponse> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddGroupMemberRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.addMember(id, request, principal));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<GroupMessageResponse> postMessage(
            @PathVariable UUID id,
            @Valid @RequestBody PostGroupMessageRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.postMessage(id, request, principal));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<Page<GroupMessageResponse>> getMessages(
            @PathVariable UUID id,
            Authentication authentication,
            @PageableDefault(size = 30) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(groupService.getMessages(id, principal, pageable));
    }

    @PostMapping("/{id}/dues/pay")
    public ResponseEntity<PaymentResponse> recordDues(
            @PathVariable UUID id,
            @Valid @RequestBody RecordDuesRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.recordDues(id, request, principal));
    }

    @GetMapping("/{id}/dues")
    public ResponseEntity<List<DuesStatusResponse>> getDuesStatus(
            @PathVariable UUID id,
            @RequestParam String month,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(groupService.getDuesStatus(id, month, principal));
    }

    @PostMapping("/{id}/dues/generate")
    public ResponseEntity<MessageResponse> generateDues(
            @PathVariable UUID id,
            @RequestParam String month,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(groupService.generateDues(id, month, principal));
    }
}
