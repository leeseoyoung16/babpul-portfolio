package com.bappul.storeservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/internal/users/{userId}")
    UserResponseDto getUserById(@PathVariable Long userId);

    // 상점 등록 시 사용자 역할을 OWNER로 변경 (user-service에 해당 엔드포인트 추가 필요)
    @PatchMapping("/internal/users/{userId}/role")
    void updateUserRole(@PathVariable Long userId, @RequestParam String role);
}
