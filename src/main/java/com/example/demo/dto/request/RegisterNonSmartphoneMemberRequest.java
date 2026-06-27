package com.example.demo.dto.request;

import com.example.demo.model.Category;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class RegisterNonSmartphoneMemberRequest {

    @NotBlank
    private String fullName;
    private String phone;
    private LocalDate dateOfBirth;
    private Category category;
}
