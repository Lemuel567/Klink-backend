package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PolishTextRequest {

    // The member's rough/simple sentences to be polished and expanded.
    @NotBlank(message = "Type a few words first")
    @Size(max = 4000)
    private String text;

    // A short label describing WHAT this text is, so the AI can match the tone
    // (e.g. "an event description", "a prayer request", "an announcement").
    @Size(max = 80)
    private String contentType;
}
