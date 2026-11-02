package com.bappul.orderservice.order;

import com.bappul.orderservice.payment.Payment;
import com.bappul.orderservice.payment.PaymentRepository;
import com.bappul.orderservice.payment.PaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPickupScheduler {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final OrderSocketPublisher orderSocketPublisher;

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void adjustPickupTimes() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> delayedOrders = orderRepository.findByStatusIn(
                List.of(OrderStatus.ACCEPTED, OrderStatus.COOKING));

        for (Order order : delayedOrders) {
            if (order.getPickupAt().isBefore(now)) {
                order.setPickupAt(order.getPickupAt().plusMinutes(5));
            }
        }
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void autoCancelUnacceptedOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> orders = orderRepository.findByStatusIn(List.of(OrderStatus.PAID));

        for (Order order : orders) {
            Payment payment = paymentRepository.findTopByOrderOrderByCreatedAtDesc(order).orElse(null);
            if (payment == null) continue;

            if (payment.getApprovedAt() != null && payment.getApprovedAt().plusMinutes(5).isBefore(now)) {
                try {
                    paymentService.cancelOrFail(order.getId(), false);
                    order.setStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);
                    orderSocketPublisher.sendCancelled(order);
                    orderSocketPublisher.sendCancelledToUser(order);
                } catch (Exception e) {
                    log.warn("[AUTO CANCEL] 주문 자동 취소 실패 - orderId={}, error={}", order.getId(), e.getMessage());
                }
            }
        }
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void autoCancelPendingOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> pendingOrders = orderRepository.findByStatusIn(List.of(OrderStatus.PENDING));

        for (Order order : pendingOrders) {
            if (order.getOrderAt().plusMinutes(10).isBefore(now)) {
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                orderSocketPublisher.sendCancelledToUser(order);
            }
        }
    }
}