package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class CreatePollRequest {

    @NotBlank
    private String question;

    @NotNull @Size(min = 2, max = 10, message = "must have between 2 and 10 options")
    private List<String> options;

    private LocalDateTime closesAt;
}
