package com.bappul.orderservice.order;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendNewOrder(Order order) {
        messagingTemplate.convertAndSend(
                "/sub/store/" + order.getStoreId(),
                new OrderMessageDto(order, OrderEventType.NEW_ORDER)
        );
    }

    public void sendStatusChanged(Order order) {
        messagingTemplate.convertAndSend(
                "/sub/user/" + order.getUserId(),
                new OrderMessageDto(order, OrderEventType.STATUS_CHANGED)
        );
    }

    public void sendCancelled(Order order) {
        messagingTemplate.convertAndSend(
                "/sub/store/" + order.getStoreId(),
                new OrderMessageDto(order, OrderEventType.CANCELED)
        );
    }
}