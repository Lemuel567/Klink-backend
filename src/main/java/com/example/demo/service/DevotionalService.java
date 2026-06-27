package com.example.demo.service;

import com.example.demo.dto.request.PostDevotionalRequest;
import com.example.demo.dto.response.DevotionalResponse;
import com.example.demo.model.Devotional;
import com.example.demo.repository.DevotionalRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.security.RoleChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DevotionalService {

    private final DevotionalRepository devotionalRepository;

    public DevotionalResponse postDevotional(PostDevotionalRequest request, MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content is required");
        }
        if (request.getDevotionalDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Devotional date is required");
        }

        Devotional devotional = Devotional.builder()
                .church(principal.getMember().getChurch())
                .title(request.getTitle())
                .content(request.getContent())
                .devotionalDate(request.getDevotionalDate())
                .postedBy(principal.getMemberId())
                .build();

        return DevotionalResponse.from(devotionalRepository.save(devotional));
    }

    @Transactional(readOnly = true)
    public Page<DevotionalResponse> getAllDevotionals(MemberPrincipal principal, Pageable pageable) {
        return devotionalRepository
                .findByChurchIdOrderByDevotionalDateDesc(principal.getChurchId(), pageable)
                .map(DevotionalResponse::from);
    }

    public void deleteDevotional(UUID devotionalId, MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);
        Devotional devotional = devotionalRepository.findByChurchIdAndId(principal.getChurchId(), devotionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Devotional not found"));
        devotionalRepository.delete(devotional);
    }
}
