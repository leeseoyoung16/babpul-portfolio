package com.babpul.orderservice.cart;

import com.babpul.orderservice.cartitem.CartItem;
import com.babpul.orderservice.client.dto.MenuDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

    @Transactional
    public Cart createOrGetCart(Long userId) {
        List<Cart> carts = cartRepository.findAllByUserIdAndStatusWithFullItems(userId, CartStatus.ACTIVE);
        if (!carts.isEmpty()) {
            return carts.get(0);
        }
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setStatus(CartStatus.ACTIVE);
        cart.setTotalPrice(0);
        return cartRepository.save(cart);
    }

    @Transactional
    public void deactivateCart(Long userId) {
        cartRepository.findAllByUserIdAndStatusWithFullItems(userId, CartStatus.ACTIVE)
                .forEach(cart -> cart.setStatus(CartStatus.CHECKED_OUT));
    }

    public CartItem createCartItem(Cart cart, MenuDto menu, int quantity) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setMenuId(menu.getId());
        item.setMenuName(menu.getName());
        item.setUnitPrice(menu.getPrice());
        item.setStoreId(menu.getStoreId());
        item.setPrepTime(menu.getPrepTime());
        item.setQuantity(quantity);
        item.calculateLineAmount();
        return item;
    }

    @Transactional
    public Cart addCartItem(Long userId, CartItem item) {
        List<Cart> carts = cartRepository.findAllByUserIdAndStatusWithFullItems(userId, CartStatus.ACTIVE);
        Cart cart = carts.stream().findFirst()
                .orElseThrow(() -> new RuntimeException("장바구니 없음"));
        cart.addItem(item);
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeCartItem(Long userId, Long itemId) {
        List<Cart> carts = cartRepository.findAllByUserIdAndStatusWithFullItems(userId, CartStatus.ACTIVE);
        Cart cart = carts.stream().findFirst()
                .orElseThrow(() -> new RuntimeException("장바구니 없음"));
        cart.getItems().removeIf(i -> i.getId().equals(itemId));
        return cartRepository.save(cart);
    }
}