package com.example.demo.dto.response;

import com.example.demo.model.Gallery;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class GalleryResponse {

    private UUID id;
    private String photoUrl;
    private String caption;
    private UUID uploadedBy;
    private LocalDateTime uploadedAt;

    public static GalleryResponse from(Gallery gallery) {
        return GalleryResponse.builder()
                .id(gallery.getId())
                .photoUrl(gallery.getPhotoUrl())
                .caption(gallery.getCaption())
                .uploadedBy(gallery.getUploadedBy())
                .uploadedAt(gallery.getUploadedAt())
                .build();
    }
}
