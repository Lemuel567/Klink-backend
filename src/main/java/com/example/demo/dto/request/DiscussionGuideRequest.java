package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiscussionGuideRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @Size(max = 300)
    private String scripture;

    @Size(max = 300)
    private String memoryVerse;

    // The sermon notes/summary to build questions from.
    @NotBlank(message = "This sermon has no notes to build a guide from")
    @Size(max = 12000)
    private String notes;
}
