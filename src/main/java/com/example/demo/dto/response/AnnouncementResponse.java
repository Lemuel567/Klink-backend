package com.example.demo.dto.response;

import com.example.demo.model.Announcement;
import com.example.demo.model.AnnouncementTargetType;
import com.example.demo.model.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class AnnouncementResponse {

    private UUID id;
    private String title;
    private String body;
    private String flyerUrl;
    private UUID postedBy;
    private LocalDateTime createdAt;

    // Targeting
    private AnnouncementTargetType targetType;
    private List<Role> targetRoles;
    private List<UUID> targetGroupIds;
    private List<UUID> targetMemberIds;
    private Boolean isTargeted;
    private int recipientCount;

    /** Whether the calling member has read this announcement (false for management views). */
    private boolean read;

    public static AnnouncementResponse from(Announcement a) {
        return from(a, false);
    }

    public static AnnouncementResponse from(Announcement a, boolean read) {
        return AnnouncementResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .body(a.getBody())
                .flyerUrl(a.getFlyerUrl())
                .postedBy(a.getPostedBy())
                .createdAt(a.getCreatedAt())
                .targetType(a.getTargetType() != null ? a.getTargetType() : AnnouncementTargetType.ALL)
                .targetRoles(a.getTargetRoles())
                .targetGroupIds(a.getTargetGroupIds())
                .targetMemberIds(a.getTargetMemberIds())
                .isTargeted(Boolean.TRUE.equals(a.getIsTargeted()))
                .recipientCount(a.getRecipientCount() != null ? a.getRecipientCount() : 0)
                .read(read)
                .build();
    }
}
