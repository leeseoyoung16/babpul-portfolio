package com.bappul.orderservice.payment;

import com.bappul.orderservice.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findTopByOrderOrderByCreatedAtDesc(Order order);
}