package com.babpul.orderservice.cart;

import com.babpul.orderservice.cartitem.CartItem;
import com.babpul.orderservice.client.StoreServiceClient;
import com.babpul.orderservice.client.dto.MenuDto;
import com.babpul.orderservice.config.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/carts")
public class CartController {

    private final CartService cartService;
    private final StoreServiceClient storeServiceClient;

    @PostMapping("/create")
    public ResponseEntity<CartResponseDto> createCart(
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        Cart cart = cartService.createOrGetCart(authUser.getUserId());
        return ResponseEntity.ok(new CartResponseDto(cart));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<String> clearCart(
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        cartService.deactivateCart(authUser.getUserId());
        return ResponseEntity.ok("장바구니가 초기화되었습니다.");
    }

    @PostMapping("/add/{menuId}")
    public ResponseEntity<CartResponseDto> addCartItem(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable Long menuId,
            @RequestParam int quantity) {

        // store-service에서 메뉴 정보 조회
        MenuDto menu = storeServiceClient.getMenu(menuId);

        Cart cart = cartService.createOrGetCart(authUser.getUserId());
        CartItem item = cartService.createCartItem(cart, menu, quantity);
        Cart updatedCart = cartService.addCartItem(authUser.getUserId(), item);

        return ResponseEntity.ok(new CartResponseDto(updatedCart));
    }

    @DeleteMapping("/remove/{itemId}")
    public ResponseEntity<CartResponseDto> removeCartItem(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable Long itemId) {
        Cart updatedCart = cartService.removeCartItem(authUser.getUserId(), itemId);
        return ResponseEntity.ok(new CartResponseDto(updatedCart));
    }
}