package com.jerry.salesmanagement.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jerry.salesmanagement.config.AiAnalysisProperties;
import com.jerry.salesmanagement.service.AiLlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleLlmClient implements AiLlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);

    private final AiAnalysisProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public OpenAiCompatibleLlmClient(AiAnalysisProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("AI analysis model is not configured");
        }

        URI endpointUri = resolveEndpointUri(properties.getBaseUrl());
        Map<String, Object> payload = Map.of(
                "model", properties.getModel().trim(),
                "temperature", properties.getTemperature(),
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            int timeoutSeconds = Math.max(5, properties.getTimeoutSeconds());
            HttpRequest.Builder builder = HttpRequest.newBuilder(endpointUri)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8));

            if (StringUtils.hasText(properties.getApiKey())) {
                builder.header("Authorization", "Bearer " + properties.getApiKey().trim());
            }

            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("AI endpoint returned HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String content = extractMessageContent(choices.get(0).path("message").path("content"));
                if (StringUtils.hasText(content)) {
                    return content.trim();
                }
            }
            throw new IllegalStateException("AI endpoint returned empty completion choices");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI request interrupted");
        } catch (IOException e) {
            log.warn("Failed to communicate with AI endpoint {}: {}", endpointUri, e.getMessage());
            throw new IllegalStateException("AI endpoint is temporarily unreachable");
        }
    }

    private String extractMessageContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        if (contentNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : contentNode) {
                if (part == null || part.isNull()) {
                    continue;
                }
                if (part.isTextual()) {
                    sb.append(part.asText(""));
                } else if (part.hasNonNull("text")) {
                    String type = part.path("type").asText("text");
                    if ("text".equalsIgnoreCase(type) || !part.has("type")) {
                        sb.append(part.path("text").asText(""));
                    }
                }
            }
            return sb.toString();
        }
        return contentNode.asText("");
    }

    private URI resolveEndpointUri(String rawBaseUrl) {
        String normalized = rawBaseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.endsWith("/chat/completions")) {
            normalized = normalized + "/chat/completions";
        }
        URI uri = URI.create(normalized);
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("AI base URL must use http or https");
        }
        return uri;
    }
}
