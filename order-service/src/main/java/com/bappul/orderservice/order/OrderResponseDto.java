package com.bappul.orderservice.order;

import com.bappul.orderservice.orderitem.OrderItemDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OrderResponseDto {
    private Long orderId;
    private String orderNumber;
    private String storeName;
    private String userName;
    private Integer totalPrice;
    private OrderStatus status;
    private OrderMode mode;
    private LocalDateTime orderAt;
    private LocalDateTime pickupAt;
    private LocalDateTime reservedAt;
    private List<OrderItemDto> orderItems;
    private String requirement;

    public OrderResponseDto(Order order) {
        this.orderId = order.getId();
        this.orderNumber = order.getOrderNumber();
        this.storeName = order.getStoreNameSnapshot();
        this.userName = order.getUserNameSnapshot();
        this.totalPrice = order.getTotalPrice();
        this.status = order.getStatus();
        this.mode = order.getMode();
        this.orderAt = order.getOrderAt();
        this.pickupAt = order.getPickupAt();
        this.reservedAt = order.getReservedAt();
        this.orderItems = order.getOrderItems() != null
                ? order.getOrderItems().stream().map(OrderItemDto::new).toList()
                : Collections.emptyList();
        this.requirement = order.getRequirement();
    }
}