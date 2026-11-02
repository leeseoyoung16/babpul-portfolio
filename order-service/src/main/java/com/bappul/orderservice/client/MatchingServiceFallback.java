package com.bappul.orderservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class MatchingServiceFallback implements MatchingServiceClient {

    @Override
    public void earnPoints(Map<String, Object> body) {
        log.warn("[Fallback] 포인트 지급 실패 - matching-service 응답 없음. body={}", body);
    }
}