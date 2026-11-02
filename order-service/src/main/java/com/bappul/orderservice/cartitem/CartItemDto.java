package com.bappul.orderservice.cartitem;

import lombok.Getter;

@Getter
public class CartItemDto {
    private Long id;
    private Long menuId;
    private String menuName;
    private Integer price;
    private Integer quantity;

    public CartItemDto(CartItem item) {
        this.id = item.getId();
        this.menuId = item.getMenuId();
        this.menuName = item.getMenuName();
        this.price = item.getUnitPrice();
        this.quantity = item.getQuantity();
    }
}
