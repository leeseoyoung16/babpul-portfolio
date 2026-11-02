package com.babpul.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "matching-service", contextId = "matchingServiceClient", fallback = MatchingServiceFallback.class)
public interface MatchingServiceClient {

    @PostMapping("/internal/points/earn")
    void earnPoints(@RequestBody Map<String, Object> body);
}