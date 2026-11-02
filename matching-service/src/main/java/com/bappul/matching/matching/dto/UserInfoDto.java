package com.bappul.matching.matching.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * user-service 에서 받아오는 유저 정보 DTO.
 * matching-service 는 gender, grade, major 만 사용한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class UserInfoDto {
    private String gender;  // "MALE" | "FEMALE"
    private Integer grade;
    private String major;
}
