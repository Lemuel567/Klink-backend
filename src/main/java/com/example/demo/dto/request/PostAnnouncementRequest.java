package com.example.demo.dto.request;

import com.example.demo.model.AnnouncementTargetType;
import com.example.demo.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class PostAnnouncementRequest {

    @NotBlank @Size(max = 200)
    private String title;

    @NotBlank @Size(max = 5000)
    private String body;

    private AnnouncementTargetType targetType = AnnouncementTargetType.ALL;

    private List<Role> targetRoles;

    private List<UUID> targetGroupIds;

    private List<UUID> targetMemberIds;
}
