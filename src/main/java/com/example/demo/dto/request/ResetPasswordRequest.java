package com.example.demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ResetPasswordRequest {

    @NotBlank @Email
    private String email;

    @NotBlank
    private String code;

    @NotBlank @Size(min = 12, message = "must be at least 12 characters")
    private String newPassword;
}
