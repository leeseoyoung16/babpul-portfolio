package com.bappul.orderservice.order;

import com.bappul.orderservice.client.StoreServiceClient;
import com.bappul.orderservice.client.dto.StoreInfoDto;
import com.bappul.orderservice.config.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final StoreServiceClient storeServiceClient;

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody OrderCreateRequestDto request) {

        Order order = orderService.createOrder(
                authUser.getUserId(),
                request.getReservedAt(),
                request.getOrderMode(),
                request.getRequirement()
        );
        return ResponseEntity.ok(new OrderResponseDto(order));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable Long orderId) {
        Order order = orderService.cancelOrder(authUser.getUserId(), orderId);
        return ResponseEntity.ok(new OrderResponseDto(order));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable Long orderId,
            @RequestParam OrderStatus nextStatus) {
        Order order = orderService.updateOrderStatus(authUser.getUserId(), orderId, nextStatus);
        return ResponseEntity.ok(new OrderResponseDto(order));
    }

    @PostMapping("/{orderId}/reject")
    public ResponseEntity<OrderResponseDto> rejectOrder(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable Long orderId) {
        Order order = orderService.rejectOrder(authUser.getUserId(), orderId);
        return ResponseEntity.ok(new OrderResponseDto(order));
    }

    @GetMapping("/user/all")
    public ResponseEntity<List<OrderResponseDto>> getAllOrdersByUser(
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(orderService.getAllOrdersByUser(authUser.getUserId()));
    }

    @GetMapping("/user/active")
    public ResponseEntity<List<OrderResponseDto>> getActiveOrdersByUser(
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(orderService.getActiveOrdersByUser(authUser.getUserId()));
    }

    @GetMapping("/store/active")
    public ResponseEntity<List<OrderResponseDto>> getActiveOrdersByStore(
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        StoreInfoDto storeInfo = storeServiceClient.getStoreByOwnerId(authUser.getUserId());
        return ResponseEntity.ok(orderService.getActiveOrdersByStore(storeInfo.getId()));
    }

    @GetMapping("/store/pending")
    public ResponseEntity<List<OrderResponseDto>> getPendingOrdersByStore(
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        StoreInfoDto storeInfo = storeServiceClient.getStoreByOwnerId(authUser.getUserId());
        return ResponseEntity.ok(orderService.getPendingOrdersByStore(storeInfo.getId()));
    }

    @GetMapping("/store/all")
    public ResponseEntity<List<OrderResponseDto>> getAllOrdersByStore(
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        StoreInfoDto storeInfo = storeServiceClient.getStoreByOwnerId(authUser.getUserId());
        return ResponseEntity.ok(orderService.getAllOrdersByStore(storeInfo.getId()));
    }
}