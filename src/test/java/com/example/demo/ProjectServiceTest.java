package com.example.demo;

import com.example.demo.dto.request.CreateProjectRequest;
import com.example.demo.dto.request.RecordProjectContributionRequest;
import com.example.demo.dto.request.UpdateProjectStatusRequest;
import com.example.demo.dto.response.ContributionResponse;
import com.example.demo.dto.response.ProjectResponse;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.NotificationService;
import com.example.demo.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock ChurchProjectRepository projectRepository;
    @Mock ProjectUpdateRepository updateRepository;
    @Mock ProjectImageRepository imageRepository;
    @Mock ProjectContributionRepository contributionRepository;
    @Mock MemberRepository memberRepository;
    @Mock NotificationService notificationService;

    @InjectMocks ProjectService projectService;

    // ── helpers ───────────────────────────────────────────────────────────────

    private MemberPrincipal principal(Role role) {
        UUID churchId = UUID.randomUUID();
        Church church = Church.builder().id(churchId).churchName("Test Church").build();
        Member member = Member.builder()
                .id(UUID.randomUUID())
                .church(church)
                .role(role)
                .status(MemberStatus.ACTIVE)
                .fullName("Test " + role)
                .hasSmartphone(true)
                .build();
        return new MemberPrincipal(member, churchId);
    }

    private ChurchProject projectOf(UUID churchId, UUID projectId, ProjectStatus status) {
        Church church = Church.builder().id(churchId).build();
        return ChurchProject.builder()
                .id(projectId)
                .church(church)
                .title("New Sanctuary")
                .description("Building a new sanctuary")
                .projectType(ProjectType.CONSTRUCTION)
                .status(status)
                .targetAmount(BigDecimal.valueOf(100_000))
                .amountRaised(BigDecimal.ZERO)
                .currency("GHS")
                .isPublic(true)
                .createdBy(UUID.randomUUID())
                .build();
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void pastor_can_create_project() {
        MemberPrincipal p = principal(Role.PASTOR);
        CreateProjectRequest req = new CreateProjectRequest();
        req.setTitle("New Hall");
        req.setDescription("Building a new hall");
        req.setProjectType(ProjectType.CONSTRUCTION);
        req.setTargetAmount(BigDecimal.valueOf(50_000));
        req.setPublic(true);

        ChurchProject saved = projectOf(p.getChurchId(), UUID.randomUUID(), ProjectStatus.PROPOSED);
        when(projectRepository.save(any())).thenReturn(saved);
        when(contributionRepository.countDistinctContributorsByProjectId(any())).thenReturn(0L);

        ProjectResponse response = projectService.createProject(req, p);
        assertThat(response.getStatus()).isEqualTo(ProjectStatus.PROPOSED);
        verify(projectRepository).save(any(ChurchProject.class));
    }

    @Test
    void new_project_always_starts_as_proposed() {
        MemberPrincipal p = principal(Role.MANAGER);
        CreateProjectRequest req = new CreateProjectRequest();
        req.setTitle("Community Garden");
        req.setDescription("Outreach project");
        req.setProjectType(ProjectType.COMMUNITY_OUTREACH);
        req.setTargetAmount(BigDecimal.valueOf(5_000));

        ChurchProject saved = projectOf(p.getChurchId(), UUID.randomUUID(), ProjectStatus.PROPOSED);
        when(projectRepository.save(any())).thenReturn(saved);
        when(contributionRepository.countDistinctContributorsByProjectId(any())).thenReturn(0L);

        ProjectResponse response = projectService.createProject(req, p);
        assertThat(response.getStatus()).isEqualTo(ProjectStatus.PROPOSED);
    }

    @Test
    void member_cannot_create_project() {
        MemberPrincipal p = principal(Role.MEMBER);
        CreateProjectRequest req = new CreateProjectRequest();
        req.setTitle("X");
        req.setDescription("Y");
        req.setProjectType(ProjectType.OTHER);
        req.setTargetAmount(BigDecimal.TEN);

        assertThatThrownBy(() -> projectService.createProject(req, p))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── status transitions ────────────────────────────────────────────────────

    @Test
    void proposed_can_move_to_approved() {
        MemberPrincipal p = principal(Role.PASTOR);
        UUID projectId = UUID.randomUUID();
        ChurchProject project = projectOf(p.getChurchId(), projectId, ProjectStatus.PROPOSED);

        when(projectRepository.findByChurchIdAndIdAndDeletedAtIsNull(p.getChurchId(), projectId))
                .thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(contributionRepository.countDistinctContributorsByProjectId(projectId)).thenReturn(0L);

        UpdateProjectStatusRequest req = new UpdateProjectStatusRequest();
        req.setStatus(ProjectStatus.APPROVED);

        ProjectResponse result = projectService.updateStatus(projectId, req, p);
        assertThat(result.getStatus()).isEqualTo(ProjectStatus.APPROVED);
    }

    @Test
    void invalid_transition_proposed_to_completed_throws_400() {
        MemberPrincipal p = principal(Role.PASTOR);
        UUID projectId = UUID.randomUUID();
        ChurchProject project = projectOf(p.getChurchId(), projectId, ProjectStatus.PROPOSED);

        when(projectRepository.findByChurchIdAndIdAndDeletedAtIsNull(p.getChurchId(), projectId))
                .thenReturn(Optional.of(project));

        UpdateProjectStatusRequest req = new UpdateProjectStatusRequest();
        req.setStatus(ProjectStatus.COMPLETED);

        assertThatThrownBy(() -> projectService.updateStatus(projectId, req, p))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void completed_project_cannot_be_updated() {
        MemberPrincipal p = principal(Role.PASTOR);
        UUID projectId = UUID.randomUUID();
        ChurchProject project = projectOf(p.getChurchId(), projectId, ProjectStatus.COMPLETED);

        when(projectRepository.findByChurchIdAndIdAndDeletedAtIsNull(p.getChurchId(), projectId))
                .thenReturn(Optional.of(project));

        UpdateProjectStatusRequest req = new UpdateProjectStatusRequest();
        req.setStatus(ProjectStatus.IN_PROGRESS);

        assertThatThrownBy(() -> projectService.updateStatus(projectId, req, p))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void cancelled_project_cannot_change_status() {
        MemberPrincipal p = principal(Role.PASTOR);
        UUID projectId = UUID.randomUUID();
        ChurchProject project = projectOf(p.getChurchId(), projectId, ProjectStatus.CANCELLED);

        when(projectRepository.findByChurchIdAndIdAndDeletedAtIsNull(p.getChurchId(), projectId))
                .thenReturn(Optional.of(project));

        UpdateProjectStatusRequest req = new UpdateProjectStatusRequest();
        req.setStatus(ProjectStatus.PROPOSED);

        assertThatThrownBy(() -> projectService.updateStatus(projectId, req, p))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── contributions ─────────────────────────────────────────────────────────

    @Test
    void only_financial_secretary_can_record_contribution() {
        MemberPrincipal manager = principal(Role.MANAGER);
        UUID projectId = UUID.randomUUID();

        RecordProjectContributionRequest req = new RecordProjectContributionRequest();
        req.setMemberId(UUID.randomUUID());
        req.setAmount(BigDecimal.valueOf(200));
        req.setContributionDate(LocalDate.now());
        req.setPaymentMethod(ContributionPaymentMethod.CASH);

        assertThatThrownBy(() -> projectService.recordContribution(projectId, req, manager))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void contribution_updates_amount_raised() {
        MemberPrincipal p = principal(Role.FINANCIAL_SECRETARY);
        UUID projectId = UUID.randomUUID();
        ChurchProject project = projectOf(p.getChurchId(), projectId, ProjectStatus.FUNDRAISING);
        UUID memberId = UUID.randomUUID();
        Member contributor = Member.builder()
                .id(memberId)
                .church(project.getChurch())
                .fullName("John Doe")
                .role(Role.MEMBER)
                .status(MemberStatus.ACTIVE)
                .hasSmartphone(true)
                .build();

        when(projectRepository.findByChurchIdAndIdAndDeletedAtIsNull(p.getChurchId(), projectId))
                .thenReturn(Optional.of(project));
        when(memberRepository.findByChurchIdAndId(p.getChurchId(), memberId))
                .thenReturn(Optional.of(contributor));
        when(contributionRepository.save(any())).thenAnswer(inv -> {
            ProjectContribution c = inv.getArgument(0);
            return ProjectContribution.builder()
                    .id(UUID.randomUUID())
                    .project(project)
                    .member(contributor)
                    .church(project.getChurch())
                    .amount(c.getAmount())
                    .currency("GHS")
                    .contributionDate(c.getContributionDate())
                    .paymentMethod(c.getPaymentMethod())
                    .recordedBy(c.getRecordedBy())
                    .build();
        });
        when(contributionRepository.sumAmountByProjectId(projectId))
                .thenReturn(BigDecimal.valueOf(500));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notificationService.notifyMember(any(), any(), any())).thenReturn(true);

        RecordProjectContributionRequest req = new RecordProjectContributionRequest();
        req.setMemberId(memberId);
        req.setAmount(BigDecimal.valueOf(500));
        req.setContributionDate(LocalDate.now());
        req.setPaymentMethod(ContributionPaymentMethod.MOBILE_MONEY);

        ContributionResponse result = projectService.recordContribution(projectId, req, p);

        assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(500));
        assertThat(project.getAmountRaised()).isEqualByComparingTo("500");
        verify(projectRepository, times(1)).save(project);
    }

    @Test
    void cannot_contribute_to_completed_project() {
        MemberPrincipal p = principal(Role.FINANCIAL_SECRETARY);
        UUID projectId = UUID.randomUUID();
        ChurchProject project = projectOf(p.getChurchId(), projectId, ProjectStatus.COMPLETED);

        when(projectRepository.findByChurchIdAndIdAndDeletedAtIsNull(p.getChurchId(), projectId))
                .thenReturn(Optional.of(project));

        RecordProjectContributionRequest req = new RecordProjectContributionRequest();
        req.setMemberId(UUID.randomUUID());
        req.setAmount(BigDecimal.valueOf(100));
        req.setContributionDate(LocalDate.now());
        req.setPaymentMethod(ContributionPaymentMethod.CASH);

        assertThatThrownBy(() -> projectService.recordContribution(projectId, req, p))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── visibility ────────────────────────────────────────────────────────────

    @Test
    void member_cannot_see_private_project() {
        MemberPrincipal p = principal(Role.MEMBER);
        UUID projectId = UUID.randomUUID();
        ChurchProject project = projectOf(p.getChurchId(), projectId, ProjectStatus.PROPOSED);
        project.setPublic(false);

        when(projectRepository.findByChurchIdAndIdAndDeletedAtIsNull(p.getChurchId(), projectId))
                .thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.getProject(projectId, p))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void pastor_can_see_private_project() {
        MemberPrincipal p = principal(Role.PASTOR);
        UUID projectId = UUID.randomUUID();
        ChurchProject project = projectOf(p.getChurchId(), projectId, ProjectStatus.PROPOSED);
        project.setPublic(false);

        when(projectRepository.findByChurchIdAndIdAndDeletedAtIsNull(p.getChurchId(), projectId))
                .thenReturn(Optional.of(project));
        when(contributionRepository.countDistinctContributorsByProjectId(projectId)).thenReturn(0L);

        ProjectResponse result = projectService.getProject(projectId, p);
        assertThat(result).isNotNull();
    }

    // ── church isolation ──────────────────────────────────────────────────────

    @Test
    void project_from_different_church_returns_404() {
        MemberPrincipal p = principal(Role.PASTOR);
        UUID projectId = UUID.randomUUID();

        when(projectRepository.findByChurchIdAndIdAndDeletedAtIsNull(p.getChurchId(), projectId))
                .thenReturn(Optional.empty()); // different church's project not visible

        assertThatThrownBy(() -> projectService.getProject(projectId, p))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void only_pastor_or_elder_can_delete_project() {
        MemberPrincipal manager = principal(Role.MANAGER);
        UUID projectId = UUID.randomUUID();

        assertThatThrownBy(() -> projectService.deleteProject(projectId, manager))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void delete_project_is_soft_delete() {
        MemberPrincipal p = principal(Role.PASTOR);
        UUID projectId = UUID.randomUUID();
        ChurchProject project = projectOf(p.getChurchId(), projectId, ProjectStatus.PROPOSED);

        when(projectRepository.findByChurchIdAndIdAndDeletedAtIsNull(p.getChurchId(), projectId))
                .thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        projectService.deleteProject(projectId, p);

        assertThat(project.getDeletedAt()).isNotNull();
    }
}