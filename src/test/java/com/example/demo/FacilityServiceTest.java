package com.example.demo;
//church facility codes for the app

import com.example.demo.dto.request.AddFacilityImageRequest;
import com.example.demo.dto.request.CreateFacilityRequest;
import com.example.demo.dto.request.UpdateFacilityRequest;
import com.example.demo.dto.response.FacilityResponse;
import com.example.demo.model.*;
import com.example.demo.repository.FacilityImageRepository;
import com.example.demo.repository.FacilityRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.FacilityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacilityServiceTest {

    @Mock FacilityRepository facilityRepository;
    @Mock FacilityImageRepository facilityImageRepository;

    @InjectMocks FacilityService facilityService;

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

    private Facility facilityFor(UUID churchId, UUID facilityId) {
        Church church = Church.builder().id(churchId).build();
        return Facility.builder()
                .id(facilityId)
                .church(church)
                .name("Main Sanctuary")
                .facilityType(FacilityType.SANCTUARY)
                .condition(FacilityCondition.GOOD)
                .build();
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void pastor_can_create_facility() {
        MemberPrincipal p = principal(Role.PASTOR);
        CreateFacilityRequest req = new CreateFacilityRequest();
        req.setName("Hall A");
        req.setFacilityType(FacilityType.HALL);
        req.setCondition(FacilityCondition.EXCELLENT);

        Facility saved = Facility.builder()
                .id(UUID.randomUUID())
                .church(p.getMember().getChurch())
                .name("Hall A")
                .facilityType(FacilityType.HALL)
                .condition(FacilityCondition.EXCELLENT)
                .build();
        when(facilityRepository.save(any())).thenReturn(saved);

        FacilityResponse response = facilityService.createFacility(req, p);

        assertThat(response.getName()).isEqualTo("Hall A");
        verify(facilityRepository).save(any(Facility.class));
    }

    @Test
    void manager_can_create_facility() {
        MemberPrincipal p = principal(Role.MANAGER);
        CreateFacilityRequest req = new CreateFacilityRequest();
        req.setName("Office Block");
        req.setFacilityType(FacilityType.OFFICE);
        req.setCondition(FacilityCondition.FAIR);

        Facility saved = Facility.builder()
                .id(UUID.randomUUID())
                .church(p.getMember().getChurch())
                .name("Office Block")
                .facilityType(FacilityType.OFFICE)
                .condition(FacilityCondition.FAIR)
                .build();
        when(facilityRepository.save(any())).thenReturn(saved);

        FacilityResponse response = facilityService.createFacility(req, p);
        assertThat(response).isNotNull();
    }

    @Test
    void member_cannot_create_facility() {
        MemberPrincipal p = principal(Role.MEMBER);
        CreateFacilityRequest req = new CreateFacilityRequest();
        req.setName("Hall B");
        req.setFacilityType(FacilityType.HALL);
        req.setCondition(FacilityCondition.GOOD);

        assertThatThrownBy(() -> facilityService.createFacility(req, p))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void elder_cannot_create_facility() {
        MemberPrincipal p = principal(Role.ELDER);
        CreateFacilityRequest req = new CreateFacilityRequest();
        req.setName("Hall C");
        req.setFacilityType(FacilityType.HALL);
        req.setCondition(FacilityCondition.GOOD);

        assertThatThrownBy(() -> facilityService.createFacility(req, p))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── get ───────────────────────────────────────────────────────────────────

    @Test
    void get_facility_not_found_returns_404() {
        MemberPrincipal p = principal(Role.PASTOR);
        UUID facilityId = UUID.randomUUID();

        when(facilityRepository.findByChurchIdAndIdAndDeletedAtIsNull(p.getChurchId(), facilityId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> facilityService.getFacility(facilityId, p))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void get_facility_cross_church_returns_404() {
        MemberPrincipal p = principal(Role.PASTOR);
        UUID facilityId = UUID.randomUUID();
        // A different church's facility won't be returned for this church's query
        when(facilityRepository.findByChurchIdAndIdAndDeletedAtIsNull(p.getChurchId(), facilityId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> facilityService.getFacility(facilityId, p))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_patches_only_provided_fields() {
        MemberPrincipal p = principal(Role.MANAGER);
        UUID facilityId = UUID.randomUUID();
        Facility existing = facilityFor(p.getChurchId(), facilityId);
        existing.setNotes("Old note");
        existing.setCondition(FacilityCondition.GOOD);

        when(facilityRepository.findByChurchIdAndIdAndDeletedAtIsNull(p.getChurchId(), facilityId))
                .thenReturn(Optional.of(existing));
        when(facilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateFacilityRequest req = new UpdateFacilityRequest();
        req.setCondition(FacilityCondition.NEEDS_REPAIR);
        // name not provided — should stay unchanged

        FacilityResponse result = facilityService.updateFacility(facilityId, req, p);

        assertThat(result.getName()).isEqualTo("Main Sanctuary");
        assertThat(result.getCondition()).isEqualTo(FacilityCondition.NEEDS_REPAIR);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void only_pastor_or_elder_can_delete() {
        MemberPrincipal manager = principal(Role.MANAGER);
        UUID facilityId = UUID.randomUUID();

        assertThatThrownBy(() -> facilityService.deleteFacility(facilityId, manager))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void pastor_delete_soft_deletes() {
        MemberPrincipal p = principal(Role.PASTOR);
        UUID facilityId = UUID.randomUUID();
        Facility facility = facilityFor(p.getChurchId(), facilityId);

        when(facilityRepository.findByChurchIdAndIdAndDeletedAtIsNull(p.getChurchId(), facilityId))
                .thenReturn(Optional.of(facility));
        when(facilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        facilityService.deleteFacility(facilityId, p);

        assertThat(facility.getDeletedAt()).isNotNull();
        verify(facilityRepository).save(facility);
    }

    // ── images ────────────────────────────────────────────────────────────────

    @Test
    void add_primary_image_clears_existing_primary() {
        MemberPrincipal p = principal(Role.MANAGER);
        UUID facilityId = UUID.randomUUID();
        Facility facility = facilityFor(p.getChurchId(), facilityId);

        when(facilityRepository.findByChurchIdAndIdAndDeletedAtIsNull(p.getChurchId(), facilityId))
                .thenReturn(Optional.of(facility));
        when(facilityImageRepository.save(any())).thenAnswer(inv -> {
            FacilityImage img = inv.getArgument(0);
            img = FacilityImage.builder()
                    .id(UUID.randomUUID())
                    .facility(facility)
                    .church(facility.getChurch())
                    .imageUrl(img.getImageUrl())
                    .isPrimary(img.isPrimary())
                    .uploadedBy(img.getUploadedBy())
                    .build();
            return img;
        });

        AddFacilityImageRequest req = new AddFacilityImageRequest();
        req.setImageUrl("https://example.com/img.jpg");
        req.setPrimary(true);

        facilityService.addImage(facilityId, req, p);

        verify(facilityImageRepository).clearPrimaryForFacility(facilityId);
        verify(facilityImageRepository).save(any(FacilityImage.class));
    }

    @Test
    void add_image_invalid_url_throws_400() {
        MemberPrincipal p = principal(Role.MANAGER);
        UUID facilityId = UUID.randomUUID();
        Facility facility = facilityFor(p.getChurchId(), facilityId);

        when(facilityRepository.findByChurchIdAndIdAndDeletedAtIsNull(p.getChurchId(), facilityId))
                .thenReturn(Optional.of(facility));

        AddFacilityImageRequest req = new AddFacilityImageRequest();
        req.setImageUrl("ftp://not-allowed.com/img.jpg");
        req.setPrimary(false);

        assertThatThrownBy(() -> facilityService.addImage(facilityId, req, p))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void delete_image_by_non_uploader_non_privileged_is_forbidden() {
        MemberPrincipal p = principal(Role.MEMBER);
        UUID facilityId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        Facility facility = facilityFor(p.getChurchId(), facilityId);

        when(facilityRepository.findByChurchIdAndIdAndDeletedAtIsNull(p.getChurchId(), facilityId))
                .thenReturn(Optional.of(facility));

        FacilityImage image = FacilityImage.builder()
                .id(imageId)
                .facility(facility)
                .imageUrl("https://x.com/a.jpg")
                .uploadedBy(UUID.randomUUID()) // different uploader
                .build();
        when(facilityImageRepository.findByIdAndFacilityId(imageId, facilityId))
                .thenReturn(Optional.of(image));

        assertThatThrownBy(() -> facilityService.deleteImage(facilityId, imageId, p))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}