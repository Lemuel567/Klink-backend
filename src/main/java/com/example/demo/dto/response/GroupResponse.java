package com.example.demo.dto.response;

import com.example.demo.model.Group;
import com.example.demo.model.GroupStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class GroupResponse {

    private UUID id;
    private String groupName;
    private String description;
    private String photoUrl;
    private BigDecimal duesAmount;
    private UUID groupAdminId;
    private String groupAdminName;
    private UUID groupFinSecId;
    private String groupFinSecName;
    private GroupStatus status;
    private long memberCount;
    private UUID createdBy;
    private LocalDateTime createdAt;

    public static GroupResponse from(Group group) {
        return from(group, 0L);
    }

    public static GroupResponse from(Group group, long memberCount) {
        return GroupResponse.builder()
                .id(group.getId())
                .groupName(group.getGroupName())
                .description(group.getDescription())
                .photoUrl(group.getPhotoUrl())
                .duesAmount(group.getDuesAmount())
                .groupAdminId(group.getGroupAdmin() != null ? group.getGroupAdmin().getId() : null)
                .groupAdminName(group.getGroupAdmin() != null ? group.getGroupAdmin().getFullName() : null)
                .groupFinSecId(group.getGroupFinSec() != null ? group.getGroupFinSec().getId() : null)
                .groupFinSecName(group.getGroupFinSec() != null ? group.getGroupFinSec().getFullName() : null)
                .status(group.getStatus())
                .memberCount(memberCount)
                .createdBy(group.getCreatedBy())
                .createdAt(group.getCreatedAt())
                .build();
    }
}
