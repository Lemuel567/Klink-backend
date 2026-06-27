package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegisterFcmTokenRequest {

    @NotBlank(message = "FCM token is required")
    private String token;
}