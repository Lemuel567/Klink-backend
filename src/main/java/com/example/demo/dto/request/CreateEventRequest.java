package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class CreateEventRequest {

    @NotBlank @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    @Size(max = 300)
    private String location;

    @Size(max = 100)
    private String category;

    @NotNull
    private LocalDateTime eventDate;
}
