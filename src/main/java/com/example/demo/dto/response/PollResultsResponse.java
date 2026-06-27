package com.example.demo.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class PollResultsResponse {

    private UUID pollId;
    private String question;
    private int totalVotes;
    private List<OptionResult> results;

    @Getter
    @Builder
    public static class OptionResult {
        private String option;
        private int votes;
        private double percentage;
    }
}
