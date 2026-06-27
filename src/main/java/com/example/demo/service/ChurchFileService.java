package com.example.demo.service;

import com.example.demo.dto.response.ChurchFileResponse;
import com.example.demo.model.ChurchFile;
import com.example.demo.repository.ChurchFileRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.security.RoleChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ChurchFileService {

    private static final int MAX_FILES = 10;

    private final ChurchFileRepository churchFileRepository;
    private final SupabaseStorageService storageService;

    public ChurchFileResponse uploadFile(MultipartFile file,
                                          String title,
                                          String category,
                                          String language,
                                          MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);

        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        if (category == null || category.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category is required");
        }

        long currentCount = churchFileRepository.countByChurchId(principal.getChurchId());
        if (currentCount >= MAX_FILES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Church has reached the maximum of " + MAX_FILES + " files. Delete one before uploading.");
        }

        // uploadPdf validates PDF type and 30 MB size limit
        String fileUrl = storageService.uploadPdf(file, "church-files/" + principal.getChurchId());

        ChurchFile churchFile = ChurchFile.builder()
                .church(principal.getMember().getChurch())
                .title(title)
                .category(category.toLowerCase())
                .language(language)
                .fileUrl(fileUrl)
                .uploadedBy(principal.getMemberId())
                .build();

        return ChurchFileResponse.from(churchFileRepository.save(churchFile));
    }

    @Transactional(readOnly = true)
    public Page<ChurchFileResponse> getFiles(String category, String language, MemberPrincipal principal, Pageable pageable) {
        UUID churchId = principal.getChurchId();

        if (category != null && language != null) {
            return churchFileRepository.findByChurchIdAndCategoryAndLanguage(churchId, category.toLowerCase(), language, pageable)
                    .map(ChurchFileResponse::from);
        } else if (category != null) {
            return churchFileRepository.findByChurchIdAndCategory(churchId, category.toLowerCase(), pageable)
                    .map(ChurchFileResponse::from);
        } else if (language != null) {
            return churchFileRepository.findByChurchIdAndLanguage(churchId, language, pageable)
                    .map(ChurchFileResponse::from);
        } else {
            return churchFileRepository.findByChurchId(churchId, pageable)
                    .map(ChurchFileResponse::from);
        }
    }

    public void deleteFile(UUID fileId, MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);

        ChurchFile file = churchFileRepository.findByChurchIdAndId(principal.getChurchId(), fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        storageService.deleteFile(file.getFileUrl());
        churchFileRepository.delete(file);
    }
}
