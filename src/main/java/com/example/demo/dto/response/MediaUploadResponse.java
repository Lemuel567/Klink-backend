package com.example.demo.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MediaUploadResponse {

    private String imageUrl;
    private String fileName;
    private long fileSize;
    private String mimeType;
}