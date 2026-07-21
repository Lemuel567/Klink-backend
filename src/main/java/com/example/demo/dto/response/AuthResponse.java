package com.example.demo.dto.response;

import com.example.demo.model.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AuthResponse {

    private String token;
    private String refreshToken;
    private UUID memberId;
    private UUID churchId;
    private String churchCode;
    private Role role;
    private String fullName;
    private String email;
    private String photoUrl;
    private boolean emailVerified;
    private boolean phoneVerified;
}
