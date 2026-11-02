package com.babpul.orderservice.randomorder;

import com.babpul.orderservice.order.OrderMode;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RandomOrderRequestDto {
    private Long storeId;
    private OrderMode mode;
    private LocalDateTime reservedAt;
    private String requirement;
}