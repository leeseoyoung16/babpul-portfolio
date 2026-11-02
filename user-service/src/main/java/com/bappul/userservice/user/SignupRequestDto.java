package com.bappul.userservice.user;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequestDto {

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이내여야 합니다.")
    private String username;

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    private UserMajor major;

    @Min(1) @Max(4)
    private Integer grade;

    @NotNull(message = "성별을 선택해주세요.")
    private UserGender gender;
}