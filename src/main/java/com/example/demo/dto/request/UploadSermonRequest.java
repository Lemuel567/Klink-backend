package com.example.demo.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class UploadSermonRequest {

    private String preacher;
    private String title;
    private String memoryVerse;
    private String scripture;
    private LocalDate sermonDate;
    private String notes;
}
