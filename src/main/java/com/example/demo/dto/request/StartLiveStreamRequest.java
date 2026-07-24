package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StartLiveStreamRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    /**
     * Whatever the leader pasted — a YouTube watch/live/youtu.be/embed/shorts URL,
     * a bare 11-char YouTube id, or a Facebook video/live post URL. The server
     * detects the platform and extracts the handle (single source of truth)
     * rather than trusting the client to do it.
     */
    @NotBlank
    @Size(max = 600)
    private String streamUrl;
}
