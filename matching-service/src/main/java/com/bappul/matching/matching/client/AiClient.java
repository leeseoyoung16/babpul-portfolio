package com.bappul.matching.matching.client;

import com.bappul.matching.matching.dto.RecommendResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
public class AiClient {

    private final WebClient webClient;
    private final Duration timeout;
    private final ObjectMapper om;

    public AiClient(
            @Value("${app.ai.base-url}") String baseUrl,
            @Value("${app.ai.timeout-ms:30000}") long timeoutMs,
            ObjectMapper om) {
        this.timeout = Duration.ofMillis(timeoutMs);
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.om = om;
    }

    public RecommendResponse recommendJsonNode(ObjectNode requestNode) {
        String rawResponse = webClient.post()
                .uri("/match/recommend")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestNode)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(200)))
                .block(this.timeout);

        log.debug("AI RAW RESPONSE: {}", rawResponse);

        try {
            return om.readValue(rawResponse, RecommendResponse.class);
        } catch (IOException e) {
            log.error("!!! FATAL JSON MAPPING ERROR !!!");
            e.printStackTrace();
            throw new RuntimeException("AI 응답 역직렬화 실패: " + e.getMessage(), e);
        }
    }
}
