package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BibleReflectionRequest {

    @NotBlank(message = "Verse reference is required")
    @Size(max = 100)
    private String reference;

    @NotBlank(message = "Verse text is required")
    @Size(max = 2000)
    private String verse;
}
