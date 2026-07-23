package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerateSermonNotesRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @Size(max = 200)
    private String preacher;

    @Size(max = 300)
    private String memoryVerse;

    @Size(max = 300)
    private String scripture;

    // The manager's brief/rough notes — the seed the AI expands. Required:
    // without it there is nothing grounded to expand, only a bare title.
    @NotBlank(message = "Add a few notes for the AI to expand on")
    @Size(max = 4000)
    private String notes;
}
