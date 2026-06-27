package com.example.demo.dto.request;

import com.example.demo.model.Category;
import com.example.demo.validation.ValidPhoneNumber;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class RegisterMemberRequest {

    @NotBlank
    private String churchCode;

    @NotBlank
    private String fullName;

    @Email
    private String email;

    @ValidPhoneNumber
    private String phoneNumber;

    @NotBlank @Size(min = 12, message = "must be at least 12 characters")
    private String password;

    private String phone;
    private LocalDate dateOfBirth;
    private Category category;

    @AssertTrue(message = "Either email or phone number must be provided")
    public boolean isContactMethodValid() {
        boolean hasEmail = email != null && !email.isBlank();
        boolean hasPhone = phoneNumber != null && !phoneNumber.isBlank();
        return hasEmail || hasPhone;
    }
}
