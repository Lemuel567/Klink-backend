package com.example.demo.dto.response;

import com.example.demo.model.GroupMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class GroupMessageResponse {

    private UUID id;
    private UUID groupId;
    private String content;
    private UUID postedBy;
    private LocalDateTime createdAt;

    public static GroupMessageResponse from(GroupMessage message) {
        return GroupMessageResponse.builder()
                .id(message.getId())
                .groupId(message.getGroup().getId())
                .content(message.getContent())
                .postedBy(message.getPostedBy())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
