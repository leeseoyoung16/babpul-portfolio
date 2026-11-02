package com.babpul.storeaiservice.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MenuDto {
    private Long id;
    private Long storeId;
    private String name;
    private int price;
    private int prepTime;
    private boolean available;
    private Boolean isSoldOut;
    private int weight;
    private String imgUrl;
}