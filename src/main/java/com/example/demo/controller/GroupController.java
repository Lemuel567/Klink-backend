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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    private static MemberPrincipal principal(Authentication auth) {
        return (MemberPrincipal) auth.getPrincipal();
    }

    // ── Group lifecycle ──────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
            @Valid @RequestBody CreateGroupRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.createGroup(request, principal(auth)));
    }

    @GetMapping
    public ResponseEntity<Page<GroupResponse>> getAllGroups(
            Authentication auth, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(groupService.getAllGroups(principal(auth), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(groupService.getGroup(id, principal(auth)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupResponse> updateGroup(
            @PathVariable UUID id, @Valid @RequestBody UpdateGroupRequest request, Authentication auth) {
        return ResponseEntity.ok(groupService.updateGroup(id, request, principal(auth)));
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GroupResponse> uploadPhoto(
            @PathVariable UUID id, @RequestParam("file") MultipartFile file, Authentication auth) {
        return ResponseEntity.ok(groupService.uploadPhoto(id, file, principal(auth)));
    }

    // ── Appointments ─────────────────────────────────────────────────────────

    @PutMapping("/{id}/admin")
    public ResponseEntity<GroupResponse> assignAdmin(
            @PathVariable UUID id, @Valid @RequestBody AddGroupMemberRequest request, Authentication auth) {
        return ResponseEntity.ok(groupService.assignAdmin(id, request, principal(auth)));
    }

    @PutMapping("/{id}/fin-sec")
    public ResponseEntity<GroupResponse> assignFinSec(
            @PathVariable UUID id, @Valid @RequestBody AddGroupMemberRequest request, Authentication auth) {
        return ResponseEntity.ok(groupService.assignFinSec(id, request, principal(auth)));
    }

    // ── Membership (group admin only) ────────────────────────────────────────

    @PostMapping("/{id}/members")
    public ResponseEntity<GroupResponse> addMember(
            @PathVariable UUID id, @Valid @RequestBody AddGroupMemberRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.addMember(id, request, principal(auth)));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id, @PathVariable UUID memberId, Authentication auth) {
        groupService.removeMember(id, memberId, principal(auth));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<GroupMemberResponse>> listMembers(
            @PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(groupService.listMembers(id, principal(auth)));
    }

    // ── Group information / announcements ────────────────────────────────────

    @PostMapping("/{id}/messages")
    public ResponseEntity<GroupMessageResponse> postMessage(
            @PathVariable UUID id, @Valid @RequestBody PostGroupMessageRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.postMessage(id, request, principal(auth)));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<Page<GroupMessageResponse>> getMessages(
            @PathVariable UUID id, Authentication auth, @PageableDefault(size = 30) Pageable pageable) {
        return ResponseEntity.ok(groupService.getMessages(id, principal(auth), pageable));
    }

    // ── Group money ──────────────────────────────────────────────────────────

    @PostMapping("/{id}/dues/pay")
    public ResponseEntity<PaymentResponse> recordDues(
            @PathVariable UUID id, @Valid @RequestBody RecordDuesRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.recordDues(id, request, principal(auth)));
    }

    @GetMapping("/{id}/dues")
    public ResponseEntity<List<DuesStatusResponse>> getDuesStatus(
            @PathVariable UUID id, @RequestParam String month, Authentication auth) {
        return ResponseEntity.ok(groupService.getDuesStatus(id, month, principal(auth)));
    }

    @PostMapping("/{id}/dues/generate")
    public ResponseEntity<MessageResponse> generateDues(
            @PathVariable UUID id, @RequestParam String month, Authentication auth) {
        return ResponseEntity.ok(groupService.generateDues(id, month, principal(auth)));
    }

    @GetMapping("/{id}/finances/summary")
    public ResponseEntity<GroupFinanceSummaryResponse> financeSummary(
            @PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(groupService.getFinanceSummary(id, principal(auth)));
    }
}
