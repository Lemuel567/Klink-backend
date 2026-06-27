package com.example.demo.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class UpdateChurchSettingsRequest {

    private String churchName;
    private String location;
    private String denomination;
    private String contactPhone;
    private String contactEmail;
    private BigDecimal welfareAmount;
}
