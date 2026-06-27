package com.example.demo.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class AddProjectImageRequest {

    @NotBlank(message = "Image URL is required")
    @Size(max = 2000)
    private String imageUrl;

    @Size(max = 500)
    private String caption;

    @JsonProperty("isPrimary")
    private boolean isPrimary = false;

    @Size(max = 100)
    private String phase;

    private UUID updateId;

    private int sortOrder = 0;
}