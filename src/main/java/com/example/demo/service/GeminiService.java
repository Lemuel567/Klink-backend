package com.example.demo.service;

import com.example.demo.config.GeminiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thin client for Google's Gemini "generateContent" REST API (free tier).
 * Stateless: one HTTP call per invocation, nothing kept running between
 * requests. Currently used only to expand sermon notes (SermonService).
 */
@Service
@Slf4j
public class GeminiService {

    private final GeminiConfig config;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiService(GeminiConfig config, RestClient.Builder restClientBuilder) {
        this.config = config;
        this.restClient = restClientBuilder.baseUrl(config.getBaseUrl()).build();
    }

    public boolean isConfigured() {
        return config.isConfigured();
    }

    /** Sends a single-turn prompt and returns the model's text response. */
    public String generateText(String prompt) {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI note generation is not configured for this server");
        }

        ObjectNode part = objectMapper.createObjectNode().put("text", prompt);
        ArrayNode parts = objectMapper.createArrayNode().add(part);
        ObjectNode content = objectMapper.createObjectNode().set("parts", parts);
        ArrayNode contents = objectMapper.createArrayNode().add(content);
        ObjectNode body = objectMapper.createObjectNode().set("contents", contents);

        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/models/{model}:generateContent?key={key}", config.getModel(), config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            log.error("Gemini generateContent failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The AI service is unavailable right now. Please try again.");
        }

        String text = response == null ? null : response
                .path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("text").asText(null);

        if (text == null || text.isBlank()) {
            log.error("Gemini returned no text. Raw response: {}", response);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The AI service did not return a result. Please try again.");
        }
        return text.trim();
    }
}
