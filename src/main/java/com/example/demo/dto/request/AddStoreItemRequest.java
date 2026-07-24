package com.example.demo.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class AddStoreItemRequest {

    @NotBlank
    private String name;
    private String description;
    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal price;
    @NotNull
    @Min(0)
    private Integer quantity;
    private String category;

    /** Multiple pictures of the item — pre-uploaded via POST /media/upload. */
    private java.util.List<String> photoUrls;
}
