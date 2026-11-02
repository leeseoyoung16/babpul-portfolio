package com.bappul.matching.client;

import com.bappul.matching.client.dto.FcmRequestDto;
import com.bappul.matching.client.dto.UserInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", contextId = "matchingUserServiceClient", fallback = UserServiceFallback.class)
public interface UserServiceClient {

    @GetMapping("/internal/users/{userId}")
    UserInfoDto getUserById(@PathVariable Long userId);

    @PostMapping("/internal/fcm/send")
    void sendFcm(@RequestBody FcmRequestDto dto);

    @PutMapping("/internal/users/{userId}/points")
    void updatePoints(@PathVariable Long userId, @RequestParam int delta);
}