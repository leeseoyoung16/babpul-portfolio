package com.babpul.orderservice.order;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class OrderCreateRequestDto {
    private LocalDateTime reservedAt;
    private OrderMode orderMode;
    private String requirement;
}