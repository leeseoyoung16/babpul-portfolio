package com.babpul.orderservice.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderMessageDto {
    private String eventType;
    private OrderResponseDto orderResponseDto;

    public OrderMessageDto(Order order, OrderEventType eventType) {
        this.eventType = eventType.name();
        this.orderResponseDto = new OrderResponseDto(order);
    }
}