package com.example.demo.dto.request;

import com.example.demo.model.PrayerVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreatePrayerRequestRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "Prayer request content is required")
    @Size(max = 5000)
    private String content;

    @NotNull(message = "Visibility is required")
    private PrayerVisibility visibility;
}
