package com.babpul.storeservice.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreRequestDto {

    @NotBlank(message = "상점 이름은 필수 입력 항목입니다.")
    private String name;

    @NotBlank(message = "상점 주소는 필수 입력 항목입니다.")
    private String address;

    @Pattern(regexp = "^(01[016789]-\\d{3,4}-\\d{4})$", message = "전화번호 형식이 올바르지 않습니다. 예) 010-1234-5678")
    private String phone;
}
