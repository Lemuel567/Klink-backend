package com.example.demo.dto.response;

import com.example.demo.model.LiveStream;
import com.example.demo.model.LiveStreamProvider;
import com.example.demo.model.LiveStreamStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class LiveStreamResponse {

    private UUID id;
    private String title;
    /** YOUTUBE or FACEBOOK — tells the app which player to embed. */
    private LiveStreamProvider provider;
    /** YouTube video id, or the full Facebook video URL. */
    private String sourceRef;
    private LiveStreamStatus status;
    private UUID startedBy;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    public static LiveStreamResponse from(LiveStream stream) {
        return LiveStreamResponse.builder()
                .id(stream.getId())
                .title(stream.getTitle())
                .provider(stream.getProvider())
                .sourceRef(stream.getSourceRef())
                .status(stream.getStatus())
                .startedBy(stream.getStartedBy())
                .startedAt(stream.getStartedAt())
                .endedAt(stream.getEndedAt())
                .build();
    }
}
