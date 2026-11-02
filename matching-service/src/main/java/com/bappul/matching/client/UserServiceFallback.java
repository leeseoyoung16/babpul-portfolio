package com.bappul.matching.client;

import com.bappul.matching.client.dto.FcmRequestDto;
import com.bappul.matching.client.dto.UserInfoDto;
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
        return dto;
    }

    @Override
    public void sendFcm(FcmRequestDto dto) {
        log.warn("UserService fallback: sendFcm skipped");
    }

    @Override
    public void updatePoints(Long userId, int delta) {
        log.warn("UserService fallback: updatePoints skipped userId={}", userId);
    }
}