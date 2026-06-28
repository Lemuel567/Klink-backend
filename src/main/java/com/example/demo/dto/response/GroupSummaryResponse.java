package com.example.demo.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class GroupSummaryResponse {

    private UUID id;
    private String groupName;
    private long memberCount;

    public static GroupSummaryResponse of(UUID id, String groupName, long memberCount) {
        return GroupSummaryResponse.builder()
                .id(id)
                .groupName(groupName)
                .memberCount(memberCount)
                .build();
    }
}
