package com.babpul.orderservice.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StoreInfoDto {
    private Long id;
    private String name;
    private Long ownerId;
    private Boolean isOpen;
    private Integer randomPrice;
}