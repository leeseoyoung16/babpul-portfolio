package com.babpul.storeservice.menu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class MenuResponseDto {
    private Long id;
    private Long storeId;
    private String name;
    private int price;
    private int prepTime;
    private boolean available;
    @JsonProperty("isSoldOut")
    private boolean isSoldOut;
    private int weight;
    private String imgUrl;

    public MenuResponseDto(Menu menu) {
        this.id        = menu.getId();
        this.storeId   = menu.getStoreId();
        this.name      = menu.getName();
        this.price     = menu.getPrice();
        this.prepTime  = menu.getPrepTime();
        this.available = menu.isAvailable();
        this.isSoldOut = menu.isSoldout();
        this.weight    = menu.getWeight();
        this.imgUrl    = menu.getImgUrl();
    }
}