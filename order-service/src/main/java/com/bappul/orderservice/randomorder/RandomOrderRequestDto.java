package com.bappul.orderservice.randomorder;

import com.bappul.orderservice.order.OrderMode;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RandomOrderRequestDto {
    private Long storeId;
    private OrderMode mode;
    private LocalDateTime reservedAt;
    private String requirement;
}