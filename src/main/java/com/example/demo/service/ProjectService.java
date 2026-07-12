package com.example.demo.service;

import com.example.demo.dto.request.*;
import com.example.demo.dto.response.*;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.security.RoleChecker;
import com.example.demo.event.NotificationEvent;
import com.example.demo.event.TargetedNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ChurchProjectRepository projectRepository;
    private final ProjectUpdateRepository updateRepository;
    private final ProjectImageRepository imageRepository;
    private final ProjectContributionRepository contributionRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ─── Projects CRUD ───────────────────────────────────────────────────────

    public ProjectResponse createProject(CreateProjectRequest request, MemberPrincipal principal) {
        // 2026-07-12: creation is Pastor + Manager ONLY (Elders post updates/images
        // on existing projects but cannot create them)
        RoleChecker.requirePastorOrManager(principal);

        ChurchProject project = ChurchProject.builder()
                .church(principal.getMember().getChurch())
                .title(request.getTitle())
                .description(request.getDescription())
                .projectType(request.getProjectType())
                .status(ProjectStatus.PROPOSED)
                .targetAmount(request.getTargetAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "GHS")
                .startDate(request.getStartDate())
                .expectedEndDate(request.getExpectedEndDate())
                .location(request.getLocation())
                .contractor(request.getContractor())
                .facilityId(request.getFacilityId())
                .isPublic(request.isPublic())
                .createdBy(principal.getMemberId())
                .build();

        ChurchProject saved = projectRepository.save(project);
        long contributors = contributionRepository.countDistinctContributorsByProjectId(saved.getId());
        return ProjectResponse.from(saved, contributors);
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> listProjects(MemberPrincipal principal,
                                               ProjectStatus status,
                                               ProjectType projectType,
                                               Boolean publicOnly,
                                               Pageable pageable) {
        UUID churchId = principal.getChurchId();
        boolean isPrivileged = RoleChecker.isPastorOrElder(principal)
                || principal.getRole() == Role.MANAGER
                || principal.getRole() == Role.FINANCIAL_SECRETARY;

        Page<ChurchProject> page;

        if (!isPrivileged) {
            // Regular members only see public projects
            if (status != null) {
                page = projectRepository.findByChurchIdAndIsPublicTrueAndStatusAndDeletedAtIsNull(churchId, status, pageable);
            } else {
                page = projectRepository.findByChurchIdAndIsPublicTrueAndDeletedAtIsNull(churchId, pageable);
            }
        } else if (status != null) {
            page = projectRepository.findByChurchIdAndStatusAndDeletedAtIsNull(churchId, status, pageable);
        } else if (projectType != null) {
            page = projectRepository.findByChurchIdAndProjectTypeAndDeletedAtIsNull(churchId, projectType, pageable);
        } else if (Boolean.TRUE.equals(publicOnly)) {
            page = projectRepository.findByChurchIdAndIsPublicTrueAndDeletedAtIsNull(churchId, pageable);
        } else {
            page = projectRepository.findByChurchIdAndDeletedAtIsNull(churchId, pageable);
        }

        return page.map(p -> {
            long count = contributionRepository.countDistinctContributorsByProjectId(p.getId());
            return ProjectResponse.from(p, count);
        });
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId, MemberPrincipal principal) {
        ChurchProject project = loadProject(projectId, principal.getChurchId());
        enforceVisibility(project, principal);
        long count = contributionRepository.countDistinctContributorsByProjectId(projectId);
        return ProjectResponse.from(project, count);
    }

    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request, MemberPrincipal principal) {
        // Editing project details is Pastor + Manager only
        RoleChecker.requirePastorOrManager(principal);
        ChurchProject project = loadProject(projectId, principal.getChurchId());
        guardTerminalStatus(project);

        if (request.getTitle() != null) project.setTitle(request.getTitle());
        if (request.getDescription() != null) project.setDescription(request.getDescription());
        if (request.getProjectType() != null) project.setProjectType(request.getProjectType());
        if (request.getTargetAmount() != null) project.setTargetAmount(request.getTargetAmount());
        if (request.getCurrency() != null) project.setCurrency(request.getCurrency());
        if (request.getStartDate() != null) project.setStartDate(request.getStartDate());
        if (request.getExpectedEndDate() != null) project.setExpectedEndDate(request.getExpectedEndDate());
        if (request.getLocation() != null) project.setLocation(request.getLocation());
        if (request.getContractor() != null) project.setContractor(request.getContractor());
        if (request.getFacilityId() != null) project.setFacilityId(request.getFacilityId());
        if (request.getIsPublic() != null) project.setPublic(request.getIsPublic());

        ChurchProject saved = projectRepository.save(project);
        long count = contributionRepository.countDistinctContributorsByProjectId(projectId);
        return ProjectResponse.from(saved, count);
    }

    public ProjectResponse updateStatus(UUID projectId, UpdateProjectStatusRequest request, MemberPrincipal principal) {
        // Status changes are Pastor + Manager; APPROVAL specifically is Pastor only
        RoleChecker.requirePastorOrManager(principal);
        if (request.getStatus() == ProjectStatus.APPROVED) {
            RoleChecker.require(principal, "Only the Pastor can approve a project", Role.PASTOR);
        }
        ChurchProject project = loadProject(projectId, principal.getChurchId());

        ProjectStatus current = project.getStatus();
        ProjectStatus next = request.getStatus();
        validateTransition(current, next);

        project.setStatus(next);

        if (next == ProjectStatus.APPROVED) {
            project.setApprovedBy(principal.getMemberId());
            project.setApprovedAt(LocalDateTime.now());
        }
        if (next == ProjectStatus.COMPLETED || next == ProjectStatus.CANCELLED) {
            project.setActualEndDate(LocalDate.now());
        }

        ChurchProject saved = projectRepository.save(project);

        // Notify all church members about major status changes — AFTER_COMMIT, off the request thread
        if (project.isPublic() && (next == ProjectStatus.APPROVED
                || next == ProjectStatus.IN_PROGRESS
                || next == ProjectStatus.COMPLETED)) {
            String body = "Project '" + project.getTitle() + "' is now " + next.name().replace("_", " ").toLowerCase();
            eventPublisher.publishEvent(new NotificationEvent(this, principal.getChurchId(), "Project Update", body));
        }

        long count = contributionRepository.countDistinctContributorsByProjectId(projectId);
        return ProjectResponse.from(saved, count);
    }

    public void deleteProject(UUID projectId, MemberPrincipal principal) {
        // Deletion is the Pastor's call alone
        RoleChecker.require(principal, "Only the Pastor can delete a project", Role.PASTOR);
        ChurchProject project = loadProject(projectId, principal.getChurchId());
        project.setDeletedAt(LocalDateTime.now());
        projectRepository.save(project);
    }

    // ─── Project Updates ──────────────────────────────────────────────────────

    public ProjectUpdateResponse postUpdate(UUID projectId, CreateProjectUpdateRequest request, MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);
        ChurchProject project = loadProject(projectId, principal.getChurchId());
        enforceVisibility(project, principal);

        ProjectUpdate update = ProjectUpdate.builder()
                .project(project)
                .church(project.getChurch())
                .title(request.getTitle())
                .content(request.getContent())
                .postedBy(principal.getMemberId())
                .build();

        ProjectUpdate saved = updateRepository.save(update);

        // Notify contributors who have contributed to this project — AFTER_COMMIT
        List<UUID> contributorIds = contributionRepository.findDistinctMemberIdsByProjectId(projectId);
        if (!contributorIds.isEmpty()) {
            eventPublisher.publishEvent(new TargetedNotificationEvent(
                    this, principal.getChurchId(), contributorIds,
                    "Project Update: " + project.getTitle(),
                    request.getTitle()));
        }

        return ProjectUpdateResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<ProjectUpdateResponse> listUpdates(UUID projectId, MemberPrincipal principal, Pageable pageable) {
        ChurchProject project = loadProject(projectId, principal.getChurchId());
        enforceVisibility(project, principal);
        return updateRepository.findByProjectIdOrderByPostedAtDesc(projectId, pageable)
                .map(ProjectUpdateResponse::from);
    }

    // ─── Project Images ───────────────────────────────────────────────────────

    public ProjectImageResponse addImage(UUID projectId, AddProjectImageRequest request, MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);
        ChurchProject project = loadProject(projectId, principal.getChurchId());

        if (request.isPrimary()) {
            imageRepository.clearPrimaryForProject(projectId);
        }

        ProjectImage image = ProjectImage.builder()
                .project(project)
                .church(project.getChurch())
                .imageUrl(request.getImageUrl())
                .caption(request.getCaption())
                .isPrimary(request.isPrimary())
                .phase(request.getPhase())
                .updateId(request.getUpdateId())
                .sortOrder(request.getSortOrder())
                .uploadedBy(principal.getMemberId())
                .build();

        return ProjectImageResponse.from(imageRepository.save(image));
    }

    @Transactional(readOnly = true)
    public List<ProjectImageResponse> getImages(UUID projectId, String phase, MemberPrincipal principal) {
        ChurchProject project = loadProject(projectId, principal.getChurchId());
        enforceVisibility(project, principal);
        List<ProjectImage> images = (phase != null)
                ? imageRepository.findByProjectIdAndPhaseOrderBySortOrderAscUploadedAtAsc(projectId, phase)
                : imageRepository.findByProjectIdOrderBySortOrderAscUploadedAtAsc(projectId);
        return images.stream().map(ProjectImageResponse::from).toList();
    }

    public void deleteImage(UUID projectId, UUID imageId, MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);
        loadProject(projectId, principal.getChurchId());
        ProjectImage image = imageRepository.findByIdAndProjectId(imageId, projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));
        imageRepository.delete(image);
    }

    public ProjectImageResponse setPrimaryImage(UUID projectId, UUID imageId, MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);
        loadProject(projectId, principal.getChurchId());
        ProjectImage image = imageRepository.findByIdAndProjectId(imageId, projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));
        imageRepository.clearPrimaryForProject(projectId);
        image.setPrimary(true);
        return ProjectImageResponse.from(imageRepository.save(image));
    }

    // ─── Contributions ────────────────────────────────────────────────────────

    public ContributionResponse recordContribution(UUID projectId, RecordProjectContributionRequest request, MemberPrincipal principal) {
        RoleChecker.requireFinancialSecretary(principal);
        ChurchProject project = loadProject(projectId, principal.getChurchId());
        guardTerminalStatus(project);

        Member member = memberRepository.findByChurchIdAndId(principal.getChurchId(), request.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        ProjectContribution contribution = ProjectContribution.builder()
                .project(project)
                .member(member)
                .church(project.getChurch())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : project.getCurrency())
                .contributionDate(request.getContributionDate())
                .paymentMethod(request.getPaymentMethod())
                .recordedBy(principal.getMemberId())
                .notes(request.getNotes())
                .build();

        contributionRepository.save(contribution);

        // Recalculate and persist amountRaised atomically in the same transaction
        BigDecimal newTotal = contributionRepository.sumAmountByProjectId(projectId);
        project.setAmountRaised(newTotal);
        projectRepository.save(project);

        // Thank the contributor — AFTER_COMMIT so a rolled-back contribution never sends thanks
        eventPublisher.publishEvent(new TargetedNotificationEvent(
                this, principal.getChurchId(), List.of(member.getId()),
                "Contribution Recorded",
                "Thank you! Your contribution of " + request.getAmount() + " to '" + project.getTitle() + "' has been recorded."));

        return ContributionResponse.from(contribution);
    }

    @Transactional(readOnly = true)
    public Page<ContributionResponse> listContributions(UUID projectId, MemberPrincipal principal, Pageable pageable) {
        ChurchProject project = loadProject(projectId, principal.getChurchId());
        boolean isPrivileged = principal.getRole() == Role.FINANCIAL_SECRETARY
                || RoleChecker.isPastorOrElder(principal)
                || principal.getRole() == Role.MANAGER;

        return contributionRepository.findByProjectIdAndChurchId(projectId, principal.getChurchId(), pageable)
                .map(c -> isPrivileged ? ContributionResponse.from(c) : ContributionResponse.fromAnonymous(c));
    }

    @Transactional(readOnly = true)
    public Page<ContributionResponse> myContributions(MemberPrincipal principal, Pageable pageable) {
        return contributionRepository.findByMemberIdAndChurchId(
                        principal.getMemberId(), principal.getChurchId(), pageable)
                .map(ContributionResponse::from);
    }

    @Transactional(readOnly = true)
    public ContributionSummaryResponse getContributionSummary(UUID projectId, MemberPrincipal principal) {
        ChurchProject project = loadProject(projectId, principal.getChurchId());
        enforceVisibility(project, principal);

        BigDecimal raised = contributionRepository.sumAmountByProjectId(projectId);
        long contributorCount = contributionRepository.countDistinctContributorsByProjectId(projectId);
        BigDecimal avg = contributionRepository.avgAmountByProjectId(projectId);
        BigDecimal max = contributionRepository.maxAmountByProjectId(projectId);
        LocalDate lastDate = contributionRepository.lastContributionDateByProjectId(projectId);

        BigDecimal target = project.getTargetAmount();
        BigDecimal remaining = target.subtract(raised).max(BigDecimal.ZERO);
        BigDecimal pct = target.compareTo(BigDecimal.ZERO) > 0
                ? raised.multiply(BigDecimal.valueOf(100)).divide(target, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return ContributionSummaryResponse.builder()
                .targetAmount(target)
                .amountRaised(raised)
                .fundingPercentage(pct)
                .remainingAmount(remaining)
                .contributorCount(contributorCount)
                .averageContribution(avg)
                .largestContribution(max)
                .mostRecentContributionDate(lastDate)
                .build();
    }

    // ─── Dashboard ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProjectDashboardResponse getDashboard(MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);
        UUID churchId = principal.getChurchId();

        Map<String, Long> statusCounts = new java.util.LinkedHashMap<>();
        for (ProjectStatus s : ProjectStatus.values()) {
            statusCounts.put(s.name(), projectRepository.countByChurchIdAndStatus(churchId, s));
        }

        BigDecimal totalTarget = projectRepository.sumTargetAmountActive(churchId);
        BigDecimal totalRaised = projectRepository.sumAmountRaisedActive(churchId);
        BigDecimal overallPct = totalTarget.compareTo(BigDecimal.ZERO) > 0
                ? totalRaised.multiply(BigDecimal.valueOf(100)).divide(totalTarget, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Projects needing attention: IN_PROGRESS with overdue expected_end_date
        List<ChurchProject> overdue = projectRepository
                .findByChurchIdAndStatusAndExpectedEndDateBeforeAndDeletedAtIsNull(
                        churchId, ProjectStatus.IN_PROGRESS, LocalDate.now());
        List<ProjectResponse> needsAttention = overdue.stream()
                .map(p -> {
                    long cnt = contributionRepository.countDistinctContributorsByProjectId(p.getId());
                    return ProjectResponse.from(p, cnt);
                }).toList();

        List<ProjectUpdateResponse> recentUpdates = updateRepository
                .findTop5ByChurchIdOrderByPostedAtDesc(churchId)
                .stream().map(ProjectUpdateResponse::from).toList();

        // Top contributors (last 90 days, top 10)
        LocalDate since = LocalDate.now().minusDays(90);
        List<Object[]> topRaw = contributionRepository.findTopContributorsSince(
                churchId, since, PageRequest.of(0, 10));
        List<ProjectDashboardResponse.TopContributorEntry> topContributors = new ArrayList<>();
        for (Object[] row : topRaw) {
            UUID memberId = (UUID) row[0];
            BigDecimal total = (BigDecimal) row[1];
            memberRepository.findByChurchIdAndId(churchId, memberId).ifPresent(m ->
                    topContributors.add(ProjectDashboardResponse.TopContributorEntry.builder()
                            .memberName(m.getFullName())
                            .totalAmount(total)
                            .build()));
        }

        return ProjectDashboardResponse.builder()
                .totalProjectsByStatus(statusCounts)
                .totalTargetAmount(totalTarget)
                .totalAmountRaised(totalRaised)
                .overallFundingPercentage(overallPct)
                .projectsNeedingAttention(needsAttention)
                .recentUpdates(recentUpdates)
                .topContributors(topContributors)
                .build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ChurchProject loadProject(UUID projectId, UUID churchId) {
        return projectRepository.findByChurchIdAndIdAndDeletedAtIsNull(churchId, projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private void enforceVisibility(ChurchProject project, MemberPrincipal principal) {
        if (!project.isPublic()) {
            boolean isPrivileged = RoleChecker.isPastorOrElder(principal)
                    || principal.getRole() == Role.MANAGER
                    || principal.getRole() == Role.FINANCIAL_SECRETARY;
            if (!isPrivileged) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Project is not public");
            }
        }
    }

    private void guardTerminalStatus(ChurchProject project) {
        if (project.getStatus() == ProjectStatus.COMPLETED || project.getStatus() == ProjectStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot modify a " + project.getStatus().name().toLowerCase() + " project");
        }
    }

    private static final Map<ProjectStatus, List<ProjectStatus>> ALLOWED_TRANSITIONS = Map.of(
            ProjectStatus.PROPOSED,    List.of(ProjectStatus.APPROVED, ProjectStatus.CANCELLED),
            ProjectStatus.APPROVED,    List.of(ProjectStatus.FUNDRAISING, ProjectStatus.IN_PROGRESS, ProjectStatus.CANCELLED),
            ProjectStatus.FUNDRAISING, List.of(ProjectStatus.IN_PROGRESS, ProjectStatus.ON_HOLD, ProjectStatus.CANCELLED),
            ProjectStatus.IN_PROGRESS, List.of(ProjectStatus.ON_HOLD, ProjectStatus.COMPLETED, ProjectStatus.CANCELLED),
            ProjectStatus.ON_HOLD,     List.of(ProjectStatus.IN_PROGRESS, ProjectStatus.CANCELLED),
            ProjectStatus.COMPLETED,   List.of(),
            ProjectStatus.CANCELLED,   List.of()
    );

    private void validateTransition(ProjectStatus from, ProjectStatus to) {
        List<ProjectStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, List.of());
        if (!allowed.contains(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot transition from " + from + " to " + to
                            + ". Allowed: " + allowed);
        }
    }
}