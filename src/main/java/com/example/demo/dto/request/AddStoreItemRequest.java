package com.example.demo.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class AddStoreItemRequest {

    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private String category;
}
