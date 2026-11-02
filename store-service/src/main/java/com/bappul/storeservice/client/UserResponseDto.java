package com.bappul.storeservice.client;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserResponseDto {
    private Long id;
    private String username;
    private String email;
    private String provider;
    private String role;
    private String major;
    private String gender;
    private Integer grade;
}
