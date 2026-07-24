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
import org.springframework.web.client.HttpStatusCodeException;
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
        } catch (HttpStatusCodeException e) {
            // 429 = free-tier quota / rate limit. Surface it as its own status
            // with a clear message rather than a generic "unavailable".
            if (e.getStatusCode().value() == 429) {
                log.warn("Gemini rate limit / quota exhausted (429): {}", e.getResponseBodyAsString());
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "The AI has reached today's free usage limit. Please try again later.");
            }
            log.error("Gemini generateContent HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The AI service is unavailable right now. Please try again.");
        } catch (RestClientException e) {
            log.error("Gemini generateContent failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The AI service is unavailable right now. Please try again.");
        }

        String text = extractText(response);
        if (text == null || text.isBlank()) {
            log.error("Gemini returned no text. Raw response: {}", response);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The AI service did not return a result. Please try again.");
        }
        return text.trim();
    }

    /**
     * Pulls the answer out of the first candidate. Thinking-capable models
     * (Gemini 3.x) can split the reply across several parts and prefix a
     * "thought" part, so we concatenate every non-thought text part instead of
     * blindly reading parts[0].
     */
    private String extractText(JsonNode response) {
        if (response == null) return null;
        JsonNode parts = response.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray()) return null;
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.path("thought").asBoolean(false)) continue; // skip reasoning
            String t = part.path("text").asText(null);
            if (t != null && !t.isBlank()) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(t);
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}
