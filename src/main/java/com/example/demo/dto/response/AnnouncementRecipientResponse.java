package com.example.demo.dto.response;

import com.example.demo.model.Member;
import com.example.demo.model.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AnnouncementRecipientResponse {

    private UUID memberId;
    private String fullName;
    private Role role;
    private boolean hasFcmToken;
    private boolean emailVerified;

    public static AnnouncementRecipientResponse from(Member m) {
        return AnnouncementRecipientResponse.builder()
                .memberId(m.getId())
                .fullName(m.getFullName())
                .role(m.getRole())
                .hasFcmToken(m.getFcmToken() != null && !m.getFcmToken().isBlank())
                .emailVerified(Boolean.TRUE.equals(m.getEmailVerified()))
                .build();
    }
}
