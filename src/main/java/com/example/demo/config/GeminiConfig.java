package com.example.demo.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Config for Google's Gemini API (free-tier text generation) — used to expand
 * a manager's brief sermon notes into a detailed summary. See CLAUDE.md §18.
 */
@Configuration
@Getter
public class GeminiConfig {

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    // Model name is configurable — Google periodically renames/retires free-tier
    // models, so a rename never needs a code change, just this one property.
    // "gemini-flash-latest" verified working (2026-07-23) against the free tier
    // for this project; "gemini-2.0-flash" returned a 0-quota rejection and
    // "gemini-1.5-flash"/"gemini-2.5-flash" 404'd as retired/unavailable.
    @Value("${gemini.model:gemini-flash-latest}")
    private String model;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
