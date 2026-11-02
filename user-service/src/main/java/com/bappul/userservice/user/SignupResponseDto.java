package com.bappul.userservice.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignupResponseDto {
    private UserResponseDto user;
    private String accessToken;
    private String refreshToken;
}