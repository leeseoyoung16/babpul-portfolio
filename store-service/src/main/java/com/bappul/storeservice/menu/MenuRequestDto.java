package com.bappul.storeservice.menu;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class MenuRequestDto {

    @NotBlank(message = "메뉴 이름은 필수로 입력해야 합니다.")
    private String name;

    @Min(value = 100, message = "가격은 최소 100원 이상이어야 합니다.")
    private int price;

    @Min(value = 1, message = "조리 시간은 최소 1분 이상이어야 합니다.")
    private int prepTime;

    private MultipartFile image;
}