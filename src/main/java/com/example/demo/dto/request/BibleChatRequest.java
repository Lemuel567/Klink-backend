package com.example.demo.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BibleChatRequest {

    @NotBlank(message = "Verse reference is required")
    @Size(max = 100)
    private String reference;

    @NotBlank(message = "Verse text is required")
    @Size(max = 2000)
    private String verse;

    @NotBlank(message = "Ask a question first")
    @Size(max = 1000)
    private String question;

    // Prior turns of the discussion (oldest first) for context.
    @Valid
    @Size(max = 8, message = "History is limited to the last 8 turns")
    private List<AskAssistantRequest.Turn> history;
}
