package com.bappul.matching.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserInfoDto {
    private Long id;
    private String username;
    private String email;
    private String fcmToken;
    private int points;
    private String role;

    /** user-service 조회 성공 여부. fallback이 사용될 때만 false로 설정된다. */
    private boolean available = true;
}