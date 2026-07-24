package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranslateRequest {

    @NotBlank(message = "Nothing to translate")
    @Size(max = 12000)
    private String text;

    // Language to translate INTO, e.g. "Twi", "Ga", "Ewe", "French".
    @NotBlank(message = "Choose a language")
    @Size(max = 40)
    private String targetLanguage;
}
