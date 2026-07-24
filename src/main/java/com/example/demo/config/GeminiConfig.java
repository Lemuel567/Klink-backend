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
    // AVOID the "-latest" full-flash alias: on 2026-07-23 "gemini-flash-latest"
    // drifted onto gemini-3.6-flash, a preview model whose free tier is only
    // 20 requests/DAY — it 429s almost immediately. "gemini-flash-lite-latest"
    // (currently gemini-3.5-flash-lite) is a stable lite model with a much
    // larger free-tier daily allowance and returns clean text for our prompts.
    // "gemini-2.0-flash"/"gemini-2.0-flash-lite" 429'd (exhausted/0-quota) and
    // "gemini-2.5-flash"/"gemini-2.5-flash-lite" 404'd as retired for this key.
    @Value("${gemini.model:gemini-flash-lite-latest}")
    private String model;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
