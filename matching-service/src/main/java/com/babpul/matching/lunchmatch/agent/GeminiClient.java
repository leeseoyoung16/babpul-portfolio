package com.babpul.matching.lunchmatch.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Component
public class GeminiClient {

    private static final int MAX_RETRIES = 3;

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    @Value("${gemini.model}")
    private String geminiModel;

    @Value("${gemini.api-version}")
    private String geminiApiVersion;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String send(Object requestBody) throws Exception {
        String url = "https://generativelanguage.googleapis.com/"
                + geminiApiVersion + "/models/" + geminiModel
                + ":generateContent?key=" + geminiApiKey;

        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) return response.body();
            if (response.statusCode() == 503 && attempt < MAX_RETRIES) {
                long waitMs = 1000L * (1 << attempt);
                log.warn("Gemini 503, {}ms 후 재시도", waitMs);
                Thread.sleep(waitMs);
                continue;
            }
            log.error("Gemini API 오류 {} - body: {}", response.statusCode(), response.body());
            throw new RuntimeException("Gemini API 오류: " + response.statusCode());
        }
        throw new RuntimeException("Gemini API 재시도 초과");
    }
}