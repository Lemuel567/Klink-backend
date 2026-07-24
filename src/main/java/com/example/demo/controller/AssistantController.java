package com.example.demo.controller;

import com.example.demo.dto.request.AskAssistantRequest;
import com.example.demo.dto.request.BibleChatRequest;
import com.example.demo.dto.request.BibleReflectionRequest;
import com.example.demo.dto.request.DiscussionGuideRequest;
import com.example.demo.dto.request.PolishTextRequest;
import com.example.demo.dto.request.TranslateRequest;
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

    /**
     * Polish/expand a member's rough text for any compose field. Open to every
     * authenticated member. The result is returned only — nothing is saved;
     * the member reviews and edits it in their form before posting.
     */
    @PostMapping("/polish")
    public ResponseEntity<Map<String, String>> polish(
            @Valid @RequestBody PolishTextRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of("polished", assistantService.polish(request, principal)));
    }

    /** Translate church content (sermon/devotional) into a local language. */
    @PostMapping("/translate")
    public ResponseEntity<Map<String, String>> translate(
            @Valid @RequestBody TranslateRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of("translated", assistantService.translate(request, principal)));
    }

    /** Generate a small-group discussion guide from a sermon's notes. */
    @PostMapping("/discussion-guide")
    public ResponseEntity<Map<String, String>> discussionGuide(
            @Valid @RequestBody DiscussionGuideRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of("guide", assistantService.discussionGuide(request, principal)));
    }

    /** A thorough explanation of the day's Bible verse. */
    @PostMapping("/bible-reflection")
    public ResponseEntity<Map<String, String>> bibleReflection(
            @Valid @RequestBody BibleReflectionRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of("reflection", assistantService.bibleReflection(request, principal)));
    }

    /** Discuss the day's Bible verse with the AI companion. */
    @PostMapping("/bible-chat")
    public ResponseEntity<Map<String, String>> bibleChat(
            @Valid @RequestBody BibleChatRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of("answer", assistantService.bibleChat(request, principal)));
    }
}
