package com.example.demo.dto.response;

import com.example.demo.model.StoreItem;
import com.example.demo.model.StoreItemStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class StoreItemResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private String category;
    private String photoUrl;
    private java.util.List<String> photoUrls;
    private StoreItemStatus status;
    private UUID createdBy;
    private LocalDateTime createdAt;

    public static StoreItemResponse from(StoreItem item) {
        return StoreItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .category(item.getCategory())
                .photoUrl(item.getPhotoUrl())
                .photoUrls(item.getPhotoUrls())
                .status(item.getStatus())
                .createdBy(item.getCreatedBy())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
