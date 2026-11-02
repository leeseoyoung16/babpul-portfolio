package com.bappul.orderservice.client;

import com.bappul.orderservice.client.dto.FcmRequestDto;
import com.bappul.orderservice.client.dto.UserInfoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * user-service 연결 실패 시 사용하는 fallback
 */
@Slf4j
@Component
public class UserServiceFallback implements UserServiceClient {

    @Override
    public UserInfoDto getUserById(Long userId) {
        UserInfoDto fallback = new UserInfoDto();
        fallback.setId(userId);
        fallback.setUsername("알 수 없음");
        return fallback;
    }

    @Override
    public String getFcmToken(Long userId) {
        log.warn("user-service 연결 실패로 FCM 토큰 조회 스킵 (userId={})", userId);
        return null; // FCM 전송 불가 → 조용히 스킵
    }

    @Override
    public void sendFcm(FcmRequestDto dto) {
        log.warn("user-service 연결 실패로 FCM 전송 스킵 (알림은 선택적 기능)");
    }
}