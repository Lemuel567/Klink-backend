package com.example.demo.controller;

import com.example.demo.dto.request.*;
import com.example.demo.dto.response.*;
import com.example.demo.model.ProjectStatus;
import com.example.demo.model.ProjectType;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // ─── Projects CRUD ────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(request, principal));
    }

    @GetMapping
    public ResponseEntity<Page<ProjectResponse>> list(
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) ProjectType projectType,
            @RequestParam(required = false) Boolean publicOnly,
            @AuthenticationPrincipal MemberPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(projectService.listProjects(principal, status, projectType, publicOnly, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(projectService.getProject(id, principal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(projectService.updateProject(id, request, principal));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ProjectResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectStatusRequest request,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(projectService.updateStatus(id, request, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal MemberPrincipal principal) {
        projectService.deleteProject(id, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ProjectDashboardResponse> dashboard(
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(projectService.getDashboard(principal));
    }

    // ─── Project Updates ──────────────────────────────────────────────────────

    @PostMapping("/{id}/updates")
    public ResponseEntity<ProjectUpdateResponse> postUpdate(
            @PathVariable UUID id,
            @Valid @RequestBody CreateProjectUpdateRequest request,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.postUpdate(id, request, principal));
    }

    @GetMapping("/{id}/updates")
    public ResponseEntity<Page<ProjectUpdateResponse>> listUpdates(
            @PathVariable UUID id,
            @AuthenticationPrincipal MemberPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(projectService.listUpdates(id, principal, pageable));
    }

    // ─── Project Images ───────────────────────────────────────────────────────

    @PostMapping("/{id}/images")
    public ResponseEntity<ProjectImageResponse> addImage(
            @PathVariable UUID id,
            @Valid @RequestBody AddProjectImageRequest request,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.addImage(id, request, principal));
    }

    @GetMapping("/{id}/images")
    public ResponseEntity<List<ProjectImageResponse>> getImages(
            @PathVariable UUID id,
            @RequestParam(required = false) String phase,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(projectService.getImages(id, phase, principal));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId,
            @AuthenticationPrincipal MemberPrincipal principal) {
        projectService.deleteImage(id, imageId, principal);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/images/{imageId}/primary")
    public ResponseEntity<ProjectImageResponse> setPrimaryImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(projectService.setPrimaryImage(id, imageId, principal));
    }

    // ─── Contributions ────────────────────────────────────────────────────────

    @PostMapping("/{id}/contributions")
    public ResponseEntity<ContributionResponse> recordContribution(
            @PathVariable UUID id,
            @Valid @RequestBody RecordProjectContributionRequest request,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.recordContribution(id, request, principal));
    }

    @GetMapping("/{id}/contributions")
    public ResponseEntity<Page<ContributionResponse>> listContributions(
            @PathVariable UUID id,
            @AuthenticationPrincipal MemberPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(projectService.listContributions(id, principal, pageable));
    }

    @GetMapping("/{id}/contributions/summary")
    public ResponseEntity<ContributionSummaryResponse> contributionSummary(
            @PathVariable UUID id,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(projectService.getContributionSummary(id, principal));
    }

    @GetMapping("/my-contributions")
    public ResponseEntity<Page<ContributionResponse>> myContributions(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(projectService.myContributions(principal, pageable));
    }
}