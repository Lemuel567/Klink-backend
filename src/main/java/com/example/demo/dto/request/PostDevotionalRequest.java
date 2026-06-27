package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class PostDevotionalRequest {

    @NotBlank @Size(max = 200)
    private String title;

    @NotBlank @Size(max = 10000)
    private String content;

    @NotNull
    private LocalDate devotionalDate;
}
