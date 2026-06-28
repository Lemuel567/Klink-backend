package com.example.demo.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PatchAnnouncementRequest {

    @Size(max = 200)
    private String title;

    @Size(max = 5000)
    private String body;
}
