package com.example.demo.service;

import com.example.demo.dto.request.AddFacilityImageRequest;
import com.example.demo.dto.request.CreateFacilityRequest;
import com.example.demo.dto.request.UpdateFacilityRequest;
import com.example.demo.dto.response.FacilityDetailResponse;
import com.example.demo.dto.response.FacilityImageResponse;
import com.example.demo.dto.response.FacilityResponse;
import com.example.demo.model.*;
import com.example.demo.repository.FacilityImageRepository;
import com.example.demo.repository.FacilityRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.security.RoleChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final FacilityImageRepository facilityImageRepository;

    public FacilityResponse createFacility(CreateFacilityRequest request, MemberPrincipal principal) {
        RoleChecker.requirePastorOrManager(principal);

        Facility facility = Facility.builder()
                .church(principal.getMember().getChurch())
                .name(request.getName())
                .description(request.getDescription())
                .facilityType(request.getFacilityType())
                .address(request.getAddress())
                .capacity(request.getCapacity())
                .yearAcquired(request.getYearAcquired())
                .estimatedValue(request.getEstimatedValue())
                .currency(request.getCurrency() != null ? request.getCurrency() : "GHS")
                .condition(request.getCondition())
                .notes(request.getNotes())
                .createdBy(principal.getMemberId())
                .build();

        return FacilityResponse.from(facilityRepository.save(facility));
    }

    @Transactional(readOnly = true)
    public Page<FacilityResponse> listFacilities(MemberPrincipal principal,
                                                  FacilityType facilityType,
                                                  FacilityCondition condition,
                                                  Boolean isActive,
                                                  Pageable pageable) {
        UUID churchId = principal.getChurchId();

        if (facilityType != null && isActive != null) {
            return facilityRepository
                    .findByChurchIdAndFacilityTypeAndIsActiveAndDeletedAtIsNull(churchId, facilityType, isActive, pageable)
                    .map(FacilityResponse::from);
        }
        if (facilityType != null) {
            return facilityRepository
                    .findByChurchIdAndFacilityTypeAndDeletedAtIsNull(churchId, facilityType, pageable)
                    .map(FacilityResponse::from);
        }
        if (condition != null) {
            return facilityRepository
                    .findByChurchIdAndConditionAndDeletedAtIsNull(churchId, condition, pageable)
                    .map(FacilityResponse::from);
        }
        if (isActive != null) {
            return facilityRepository
                    .findByChurchIdAndIsActiveAndDeletedAtIsNull(churchId, isActive, pageable)
                    .map(FacilityResponse::from);
        }
        return facilityRepository
                .findByChurchIdAndDeletedAtIsNull(churchId, pageable)
                .map(FacilityResponse::from);
    }

    @Transactional(readOnly = true)
    public FacilityDetailResponse getFacility(UUID facilityId, MemberPrincipal principal) {
        Facility facility = loadFacility(facilityId, principal.getChurchId());
        List<FacilityImageResponse> images = facilityImageRepository
                .findByFacilityIdOrderBySortOrderAscUploadedAtAsc(facilityId)
                .stream().map(FacilityImageResponse::from).toList();
        return FacilityDetailResponse.builder()
                .facility(FacilityResponse.from(facility))
                .images(images)
                .build();
    }

    public FacilityResponse updateFacility(UUID facilityId, UpdateFacilityRequest request, MemberPrincipal principal) {
        RoleChecker.requirePastorOrManager(principal);
        Facility facility = loadFacility(facilityId, principal.getChurchId());

        if (request.getName() != null) facility.setName(request.getName());
        if (request.getDescription() != null) facility.setDescription(request.getDescription());
        if (request.getFacilityType() != null) facility.setFacilityType(request.getFacilityType());
        if (request.getAddress() != null) facility.setAddress(request.getAddress());
        if (request.getCapacity() != null) facility.setCapacity(request.getCapacity());
        if (request.getYearAcquired() != null) facility.setYearAcquired(request.getYearAcquired());
        if (request.getEstimatedValue() != null) facility.setEstimatedValue(request.getEstimatedValue());
        if (request.getCurrency() != null) facility.setCurrency(request.getCurrency());
        if (request.getCondition() != null) facility.setCondition(request.getCondition());
        if (request.getIsActive() != null) facility.setActive(request.getIsActive());
        if (request.getNotes() != null) facility.setNotes(request.getNotes());

        return FacilityResponse.from(facilityRepository.save(facility));
    }

    public void deleteFacility(UUID facilityId, MemberPrincipal principal) {
        RoleChecker.requirePastorOrElder(principal);
        Facility facility = loadFacility(facilityId, principal.getChurchId());
        facility.setDeletedAt(LocalDateTime.now());
        facilityRepository.save(facility);
    }

    public FacilityImageResponse addImage(UUID facilityId, AddFacilityImageRequest request, MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);
        Facility facility = loadFacility(facilityId, principal.getChurchId());
        validateImageUrl(request.getImageUrl());

        if (request.isPrimary()) {
            facilityImageRepository.clearPrimaryForFacility(facilityId);
        }

        FacilityImage image = FacilityImage.builder()
                .facility(facility)
                .church(facility.getChurch())
                .imageUrl(request.getImageUrl())
                .caption(request.getCaption())
                .isPrimary(request.isPrimary())
                .uploadedBy(principal.getMemberId())
                .sortOrder(request.getSortOrder())
                .build();

        return FacilityImageResponse.from(facilityImageRepository.save(image));
    }

    @Transactional(readOnly = true)
    public List<FacilityImageResponse> getImages(UUID facilityId, MemberPrincipal principal) {
        loadFacility(facilityId, principal.getChurchId());
        return facilityImageRepository
                .findByFacilityIdOrderBySortOrderAscUploadedAtAsc(facilityId)
                .stream().map(FacilityImageResponse::from).toList();
    }

    public void deleteImage(UUID facilityId, UUID imageId, MemberPrincipal principal) {
        loadFacility(facilityId, principal.getChurchId());
        FacilityImage image = facilityImageRepository.findByIdAndFacilityId(imageId, facilityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));

        boolean isPrivileged = principal.getRole() == Role.PASTOR || principal.getRole() == Role.MANAGER;
        boolean isUploader = principal.getMemberId().equals(image.getUploadedBy());
        if (!isPrivileged && !isUploader) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        facilityImageRepository.delete(image);
    }

    public FacilityImageResponse setPrimaryImage(UUID facilityId, UUID imageId, MemberPrincipal principal) {
        RoleChecker.requirePastorOrManager(principal);
        loadFacility(facilityId, principal.getChurchId());
        FacilityImage image = facilityImageRepository.findByIdAndFacilityId(imageId, facilityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));

        facilityImageRepository.clearPrimaryForFacility(facilityId);
        image.setPrimary(true);
        return FacilityImageResponse.from(facilityImageRepository.save(image));
    }

    private Facility loadFacility(UUID facilityId, UUID churchId) {
        return facilityRepository.findByChurchIdAndIdAndDeletedAtIsNull(churchId, facilityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facility not found"));
    }

    private void validateImageUrl(String url) {
        if (url == null || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image URL must be a valid HTTP/HTTPS URL");
        }
    }
}