package com.example.demo.dto.response;

import com.example.demo.model.GroupMember;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A member as seen inside a group roster. Mirrors the church directory view —
 * name + phone + photo only — plus the member's group role flags.
 */
@Getter
@Builder
public class GroupMemberResponse {

    private UUID memberId;
    private String fullName;
    private String phone;
    private String photoUrl;
    private boolean isAdmin;
    private boolean isFinSec;
    private LocalDateTime joinedAt;

    public static GroupMemberResponse from(GroupMember gm, UUID adminId, UUID finSecId) {
        UUID mid = gm.getMember().getId();
        return GroupMemberResponse.builder()
                .memberId(mid)
                .fullName(gm.getMember().getFullName())
                .phone(gm.getMember().getPhone())
                .photoUrl(gm.getMember().getPhotoUrl())
                .isAdmin(adminId != null && adminId.equals(mid))
                .isFinSec(finSecId != null && finSecId.equals(mid))
                .joinedAt(gm.getJoinedAt())
                .build();
    }
}
