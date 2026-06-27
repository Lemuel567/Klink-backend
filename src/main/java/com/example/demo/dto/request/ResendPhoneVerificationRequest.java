package com.example.demo.dto.request;

import com.example.demo.validation.ValidPhoneNumber;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ResendPhoneVerificationRequest {

    @NotBlank
    @ValidPhoneNumber
    private String phoneNumber;
}
