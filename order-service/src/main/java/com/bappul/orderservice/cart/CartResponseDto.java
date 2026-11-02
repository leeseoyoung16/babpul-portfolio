package com.bappul.orderservice.cart;

import com.bappul.orderservice.cartitem.CartItemDto;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class CartResponseDto {
    private Long id;
    private CartStatus cartStatus;
    private Integer totalPrice;
    private LocalDateTime updatedAt;
    private Long userId;
    private List<CartItemDto> items;

    public CartResponseDto(Cart cart) {
        this.id = cart.getId();
        this.cartStatus = cart.getStatus();
        this.totalPrice = cart.getTotalPrice();
        this.updatedAt = LocalDateTime.now();
        this.userId = cart.getUserId();
        this.items = cart.getItems().stream().map(CartItemDto::new).toList();
    }
}