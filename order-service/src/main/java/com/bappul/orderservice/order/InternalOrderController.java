package com.bappul.orderservice.order;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/orders")
public class InternalOrderController {

    private final OrderRepository orderRepository;

    private static final List<OrderStatus> ACTIVE_STATUSES =
            List.of(OrderStatus.PAID, OrderStatus.ACCEPTED, OrderStatus.COOKING, OrderStatus.READY);

    @GetMapping("/store/{storeId}/has-active")
    public ResponseEntity<Boolean> hasActiveOrders(@PathVariable Long storeId) {
        return ResponseEntity.ok(orderRepository.existsByStoreIdAndStatusIn(storeId, ACTIVE_STATUSES));
    }
}