package com.example.demo.dto.request;

import com.example.demo.validation.ValidPhoneNumber;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    @Email
    private String email;

    @ValidPhoneNumber
    private String phoneNumber;

    @NotBlank
    private String password;

    @AssertTrue(message = "Either email or phone number must be provided")
    public boolean isContactMethodValid() {
        boolean hasEmail = email != null && !email.isBlank();
        boolean hasPhone = phoneNumber != null && !phoneNumber.isBlank();
        return hasEmail || hasPhone;
    }
}
