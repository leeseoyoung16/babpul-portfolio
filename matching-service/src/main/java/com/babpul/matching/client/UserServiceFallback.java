package com.babpul.matching.client;

import com.babpul.matching.client.dto.FcmRequestDto;
import com.babpul.matching.client.dto.UserInfoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserServiceFallback implements UserServiceClient {

    @Override
    public UserInfoDto getUserById(Long userId) {
        log.warn("UserService fallback: getUserById userId={}", userId);
        UserInfoDto dto = new UserInfoDto();
        dto.setId(userId);
        dto.setPoints(0);
        dto.setAvailable(false);
        return dto;
    }

    @Override
    public void sendFcm(FcmRequestDto dto) {
        log.warn("UserService fallback: sendFcm skipped");
    }

    @Override
    public void updatePoints(Long userId, int delta) {
        log.warn("UserService fallback: updatePoints failed userId={}", userId);
        throw new IllegalStateException("포인트 잔액 갱신에 실패했습니다. 잠시 후 다시 시도해주세요.");
    }
}