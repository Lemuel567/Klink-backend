package com.example.demo.dto.request;

import com.example.demo.model.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AssignRoleRequest {

    @NotNull(message = "role is required")
    private Role role;
}
