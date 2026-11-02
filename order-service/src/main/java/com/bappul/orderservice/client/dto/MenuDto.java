package com.bappul.orderservice.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MenuDto {
    private Long id;
    private String name;
    private Integer price;
    private Long storeId;
    private Boolean isSoldOut;
    private Integer prepTime;
}