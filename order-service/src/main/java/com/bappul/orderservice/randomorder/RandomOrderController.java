package com.bappul.orderservice.randomorder;

import com.bappul.orderservice.config.AuthenticatedUser;
import com.bappul.orderservice.order.Order;
import com.bappul.orderservice.order.OrderResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders/random")
public class RandomOrderController {

    private final RandomOrderService randomOrderService;

    @PostMapping
    public ResponseEntity<OrderResponseDto> createRandomOrder(
            @RequestBody RandomOrderRequestDto request,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        Order order = randomOrderService.createRandomOrder(authUser, request);
        return ResponseEntity.ok(new OrderResponseDto(order));
    }

}