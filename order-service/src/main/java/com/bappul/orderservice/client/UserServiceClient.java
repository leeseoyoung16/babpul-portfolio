package com.bappul.orderservice.client;

import com.bappul.orderservice.client.dto.FcmRequestDto;
import com.bappul.orderservice.client.dto.UserInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", fallback = UserServiceFallback.class)
public interface UserServiceClient {

    @GetMapping("/internal/users/{userId}")
    UserInfoDto getUserById(@PathVariable Long userId);

    @GetMapping("/internal/users/{userId}/fcm-token")
    String getFcmToken(@PathVariable Long userId);

    @PostMapping("/internal/fcm/send")
    void sendFcm(@RequestBody FcmRequestDto dto);
}