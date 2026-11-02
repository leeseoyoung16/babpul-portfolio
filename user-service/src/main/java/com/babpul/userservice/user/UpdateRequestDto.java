package com.babpul.userservice.user;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRequestDto {

    @Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이내여야 합니다.")
    private String username;

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    private UserGender gender;

    private UserMajor major;

    @Min(1) @Max(4)
    private Integer grade;
}