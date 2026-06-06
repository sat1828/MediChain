package com.medichain.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaudeApiClient {

    private final WebClient webClient;

    @Value("${medichain.ai.claude.api-url}")
    private String apiUrl;

    @Value("${medichain.ai.claude.api-key}")
    private String apiKey;

    @Value("${medichain.ai.claude.model}")
    private String model;

    @Value("${medichain.ai.claude.max-tokens:2048}")
    private int maxTokens;

    @Value("${medichain.ai.forecasting.api-timeout-seconds:60}")
    private int timeoutSeconds;

    public ClaudeResponse sendMessage(String systemPrompt, String userPrompt) {
        var request = ClaudeRequest.builder()
            .model(model)
            .maxTokens(maxTokens)
            .system(systemPrompt)
            .messages(List.of(new ClaudeRequest.Message("user", userPrompt)))
            .build();

        try {
            var response = webClient.post()
                .uri(apiUrl)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ClaudeResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();

            if (response == null || response.content() == null || response.content().isEmpty()) {
                throw new RuntimeException("Empty response from Claude API");
            }

            return response;
        } catch (Exception e) {
            log.error("Claude API call failed: {}", e.getMessage());
            throw new RuntimeException("AI forecast generation failed: " + e.getMessage(), e);
        }
    }

    @Builder
    public record ClaudeRequest(
        String model,
        @JsonProperty("max_tokens") int maxTokens,
        String system,
        List<Message> messages
    ) {
        public record Message(String role, String content) {}
    }

    public record ClaudeResponse(
        String id,
        String model,
        List<ContentBlock> content,
        Usage usage
    ) {
        public record ContentBlock(String type, String text) {}
        public record Usage(
            @JsonProperty("input_tokens") int inputTokens,
            @JsonProperty("output_tokens") int outputTokens
        ) {}
    }
}
