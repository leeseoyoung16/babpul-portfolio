package com.babpul.orderservice.orderitem;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OrderItemDto {
    private Long id;
    private String menuName;
    private Integer quantity;
    private Integer priceSnap;
    private Integer lineAmount;
    private Long menuId;
    private String realName;
    private Integer realPrice;
    private Integer randomPrice;

    public OrderItemDto(OrderItem orderItem) {
        this.id = orderItem.getId();
        this.menuName = orderItem.getMenuName();
        this.quantity = orderItem.getQuantity();
        this.priceSnap = orderItem.getPriceSnap();
        this.lineAmount = orderItem.getLineAmount();
        this.menuId = orderItem.getMenuId();
        this.realName = orderItem.getRealName();
        this.realPrice = orderItem.getRealPrice();
        this.randomPrice = orderItem.getRandomPrice();
    }
}