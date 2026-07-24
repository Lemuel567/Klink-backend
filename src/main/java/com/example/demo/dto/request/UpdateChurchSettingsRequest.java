package com.example.demo.dto.request;

import jakarta.validation.constraints.DecimalMin;
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
    // Reject a NEGATIVE welfare amount at the edge (0 stays valid = "disabled").
    // recordWelfare also guards <= 0 before dividing, so this is defense-in-depth.
    @DecimalMin(value = "0.00")
    private BigDecimal welfareAmount;
}
