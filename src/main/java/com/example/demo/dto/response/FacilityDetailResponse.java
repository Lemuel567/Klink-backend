package com.example.demo.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FacilityDetailResponse {

    private FacilityResponse facility;
    private List<FacilityImageResponse> images;
}