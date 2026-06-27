package com.example.demo.service;

import com.example.demo.dto.request.UpdateChurchSettingsRequest;
import com.example.demo.dto.response.ChurchResponse;
import com.example.demo.model.Church;
import com.example.demo.model.Role;
import com.example.demo.repository.ChurchRepository;
import com.example.demo.security.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ChurchService {

    private final ChurchRepository churchRepository;
    private final SupabaseStorageService storageService;

    @Transactional(readOnly = true)
    public ChurchResponse getChurchSettings(MemberPrincipal principal) {
        Church church = churchRepository.findById(principal.getChurchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Church not found"));
        return ChurchResponse.from(church);
    }

    public ChurchResponse updateChurchSettings(UpdateChurchSettingsRequest request, MemberPrincipal principal) {
        Role role = principal.getRole();

        if (role != Role.PASTOR && role != Role.ELDER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a Pastor or Elder can update church settings");
        }

        Church church = churchRepository.findById(principal.getChurchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Church not found"));

        if (request.getChurchName() != null) church.setChurchName(request.getChurchName());
        if (request.getLocation() != null) church.setLocation(request.getLocation());
        if (request.getDenomination() != null) church.setDenomination(request.getDenomination());
        if (request.getContactPhone() != null) church.setContactPhone(request.getContactPhone());
        if (request.getContactEmail() != null) church.setContactEmail(request.getContactEmail());
        if (request.getWelfareAmount() != null) church.setWelfareAmount(request.getWelfareAmount());

        return ChurchResponse.from(churchRepository.save(church));
    }

    public String regenerateChurchCode(MemberPrincipal principal) {
        if (principal.getRole() != Role.PASTOR && principal.getRole() != Role.ELDER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only Pastor or Elder can regenerate the church code");
        }

        Church church = churchRepository.findById(principal.getChurchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Church not found"));

        String newCode = generateUniqueChurchCode();
        church.setChurchCode(newCode);
        churchRepository.save(church);
        return newCode;
    }

    private String generateUniqueChurchCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        String code;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            code = sb.toString();
        } while (churchRepository.existsByChurchCode(code));
        return code;
    }

    public ChurchResponse deleteChurch(MemberPrincipal principal) {
        if (principal.getRole() != Role.ELDER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only an Elder can schedule this church for deletion");
        }

        Church church = churchRepository.findById(principal.getChurchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Church not found"));

        if (church.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Church is already scheduled for deletion");
        }

        church.setDeletedAt(LocalDateTime.now());
        return ChurchResponse.from(churchRepository.save(church));
    }

    public ChurchResponse restoreChurch(MemberPrincipal principal) {
        if (principal.getRole() != Role.ELDER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only an Elder can restore a church");
        }

        Church church = churchRepository.findById(principal.getChurchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Church not found"));

        if (church.getDeletedAt() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Church is not scheduled for deletion");
        }

        if (church.getDeletedAt().plusDays(30).isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE,
                    "The 30-day restoration window has expired. This church can no longer be restored.");
        }

        church.setDeletedAt(null);
        return ChurchResponse.from(churchRepository.save(church));
    }

    public String uploadChurchPhoto(MultipartFile file, MemberPrincipal principal) {
        Role role = principal.getRole();
        if (role != Role.PASTOR && role != Role.ELDER && role != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Church church = churchRepository.findById(principal.getChurchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Church not found"));

        if (church.getPhotoUrl() != null) {
            storageService.deleteFile(church.getPhotoUrl());
        }

        String url = storageService.uploadImage(file, "churches/" + principal.getChurchId());
        church.setPhotoUrl(url);
        churchRepository.save(church);

        return url;
    }
}
