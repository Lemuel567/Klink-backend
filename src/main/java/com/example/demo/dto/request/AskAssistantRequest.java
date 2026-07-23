package com.example.demo.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AskAssistantRequest {

    @NotBlank(message = "Ask a question first")
    @Size(max = 1000)
    private String question;

    /** Last few turns of the conversation, oldest first — gives the model context. */
    @Valid
    @Size(max = 8, message = "History is limited to the last 8 turns")
    private List<Turn> history;

    @Getter
    @Setter
    public static class Turn {
        @NotBlank
        @Pattern(regexp = "user|assistant", message = "Turn role must be 'user' or 'assistant'")
        private String role;

        @NotBlank
        @Size(max = 1500)
        private String text;
    }
}
