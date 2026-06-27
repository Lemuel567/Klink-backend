package com.example.demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegisterChurchRequest {

    @NotBlank
    private String churchName;

    @NotBlank
    private String location;

    @NotBlank
    private String denomination;

    private String contactPhone;

    @Email
    private String contactEmail;

    @NotBlank
    private String pastorName;

    @NotBlank @Email
    private String pastorEmail;

    @NotBlank @Size(min = 12, message = "must be at least 12 characters")
    private String pastorPassword;

    private String pastorPhone;
}
