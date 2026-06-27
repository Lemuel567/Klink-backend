package com.example.demo.service;

import com.example.demo.dto.response.GalleryResponse;
import com.example.demo.model.Gallery;
import com.example.demo.repository.GalleryRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.security.RoleChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GalleryService {

    private final GalleryRepository galleryRepository;
    private final SupabaseStorageService storageService;

    public GalleryResponse uploadPhoto(MultipartFile photo, String caption, MemberPrincipal principal) {
        RoleChecker.requireManager(principal);

        // uploadImage validates that it is an image — rejects videos and other file types
        String photoUrl = storageService.uploadImage(photo, "gallery/" + principal.getChurchId());

        Gallery gallery = Gallery.builder()
                .church(principal.getMember().getChurch())
                .photoUrl(photoUrl)
                .caption(caption)
                .uploadedBy(principal.getMemberId())
                .build();

        return GalleryResponse.from(galleryRepository.save(gallery));
    }

    @Transactional(readOnly = true)
    public Page<GalleryResponse> getAllPhotos(MemberPrincipal principal, Pageable pageable) {
        return galleryRepository
                .findByChurchIdOrderByUploadedAtDesc(principal.getChurchId(), pageable)
                .map(GalleryResponse::from);
    }
}
