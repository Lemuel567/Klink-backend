package com.example.demo.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AddFacilityImageRequest {

    @NotBlank(message = "Image URL is required")
    @Size(max = 2000)
    private String imageUrl;

    @Size(max = 500)
    private String caption;

    @JsonProperty("isPrimary")
    private boolean isPrimary = false;

    private int sortOrder = 0;
}