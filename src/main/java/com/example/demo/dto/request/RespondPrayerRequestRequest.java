package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RespondPrayerRequestRequest {

    @NotBlank(message = "Response is required")
    @Size(max = 5000)
    private String response;
}
