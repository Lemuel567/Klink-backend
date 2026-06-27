package com.example.demo.dto.response;

import com.example.demo.model.Poll;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class PollResponse {

    private UUID id;
    private String question;
    private List<String> options;
    private LocalDateTime closesAt;
    private boolean open;
    private boolean voted;
    private UUID createdBy;
    private LocalDateTime createdAt;

    public static PollResponse from(Poll poll, boolean voted) {
        boolean open = poll.getClosesAt() == null || poll.getClosesAt().isAfter(LocalDateTime.now());
        return PollResponse.builder()
                .id(poll.getId())
                .question(poll.getQuestion())
                .options(poll.getOptions())
                .closesAt(poll.getClosesAt())
                .open(open)
                .voted(voted)
                .createdBy(poll.getCreatedBy())
                .createdAt(poll.getCreatedAt())
                .build();
    }
}
