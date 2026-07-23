package com.example.demo.controller;

import com.example.demo.dto.request.AskAssistantRequest;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.AssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    /** "Ask Klink" — open to every authenticated member; church-agnostic app help. */
    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> ask(
            @Valid @RequestBody AskAssistantRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of("answer", assistantService.ask(request, principal)));
    }
}
