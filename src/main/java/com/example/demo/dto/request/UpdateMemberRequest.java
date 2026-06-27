package com.example.demo.dto.request;

import com.example.demo.model.Category;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class UpdateMemberRequest {

    @Size(min = 1, max = 200, message = "Full name must be between 1 and 200 characters")
    private String fullName;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @Email(message = "Email must be valid")
    @Size(max = 255)
    private String email;

    private LocalDate dateOfBirth;
    private Category category;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    private LocalDate baptismDate;
    private LocalDate membershipDate;
}