package com.bappul.storeservice.client;

import org.springframework.stereotype.Component;

@Component
public class UserServiceFallback implements UserServiceClient {

    @Override
    public UserResponseDto getUserById(Long userId) {
        throw new RuntimeException("user-service 연결 불가 - 사용자 정보를 조회할 수 없습니다.");
    }

    @Override
    public void updateUserRole(Long userId, String role) {
        throw new RuntimeException("user-service 연결 불가 - 사용자 역할을 변경할 수 없습니다.");
    }
}
