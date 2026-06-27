package com.example.demo.service;

import com.example.demo.dto.request.CreateHallOfFameRequest;
import com.example.demo.dto.request.UpdateHallOfFameRequest;
import com.example.demo.dto.response.HallOfFameResponse;
import com.example.demo.model.HallOfFame;
import com.example.demo.model.Member;
import com.example.demo.model.Role;
import com.example.demo.repository.HallOfFameRepository;
import com.example.demo.repository.MemberRepository;
import com.example.demo.security.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class HallOfFameService {

    private final HallOfFameRepository hallOfFameRepository;
    private final MemberRepository memberRepository;
    private final SupabaseStorageService storageService;

    public HallOfFameResponse createEntry(CreateHallOfFameRequest request,
                                           MultipartFile photo,
                                           MemberPrincipal principal) {
        requirePrivileged(principal);

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }

        Member member = null;
        if (request.getMemberId() != null) {
            member = memberRepository.findByChurchIdAndId(principal.getChurchId(), request.getMemberId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        }

        String photoUrl = null;
        if (photo != null && !photo.isEmpty()) {
            photoUrl = storageService.uploadImage(photo, "hall-of-fame/" + principal.getChurchId());
        }

        HallOfFame entry = HallOfFame.builder()
                .church(principal.getMember().getChurch())
                .member(member)
                .title(request.getTitle())
                .description(request.getDescription())
                .photoUrl(photoUrl)
                .postedBy(principal.getMemberId())
                .build();

        return HallOfFameResponse.from(hallOfFameRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public Page<HallOfFameResponse> getAllEntries(MemberPrincipal principal, Pageable pageable) {
        return hallOfFameRepository
                .findByChurchIdOrderByCreatedAtDesc(principal.getChurchId(), pageable)
                .map(HallOfFameResponse::from);
    }

    public HallOfFameResponse updateEntry(UUID entryId,
                                           UpdateHallOfFameRequest request,
                                           MultipartFile photo,
                                           MemberPrincipal principal) {
        requirePrivileged(principal);

        HallOfFame entry = hallOfFameRepository.findByChurchIdAndId(principal.getChurchId(), entryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found"));

        if (request.getTitle() != null) entry.setTitle(request.getTitle());
        if (request.getDescription() != null) entry.setDescription(request.getDescription());

        if (request.getMemberId() != null) {
            Member member = memberRepository.findByChurchIdAndId(
                            principal.getChurchId(), request.getMemberId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
            entry.setMember(member);
        }

        if (photo != null && !photo.isEmpty()) {
            if (entry.getPhotoUrl() != null) {
                storageService.deleteFile(entry.getPhotoUrl());
            }
            entry.setPhotoUrl(storageService.uploadImage(photo, "hall-of-fame/" + principal.getChurchId()));
        }

        return HallOfFameResponse.from(hallOfFameRepository.save(entry));
    }

    public void deleteEntry(UUID entryId, MemberPrincipal principal) {
        requirePrivileged(principal);

        HallOfFame entry = hallOfFameRepository.findByChurchIdAndId(principal.getChurchId(), entryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found"));

        if (entry.getPhotoUrl() != null) {
            storageService.deleteFile(entry.getPhotoUrl());
        }

        hallOfFameRepository.delete(entry);
    }

    private void requirePrivileged(MemberPrincipal principal) {
        Role role = principal.getRole();
        if (role != Role.PASTOR && role != Role.ELDER && role != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a Pastor, Elder, or Manager can manage Hall of Fame entries");
        }
    }
}
