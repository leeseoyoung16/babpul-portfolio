package com.babpul.orderservice.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStoreIdAndStatusOrderByOrderAtAsc(Long storeId, OrderStatus status);
    List<Order> findByStoreIdOrderByOrderAtDesc(Long storeId);
    List<Order> findByStoreIdAndStatusInOrderByPickupAtAsc(Long storeId, List<OrderStatus> statuses);
    List<Order> findByStatusIn(List<OrderStatus> statuses);

    boolean existsByStoreIdAndStatusIn(Long storeId, List<OrderStatus> statuses);

    @Query("SELECT COALESCE(MAX(o.dailySeq), 0) FROM Order o WHERE o.storeId = :storeId AND o.orderDate = :date")
    int findMaxDailySeq(@Param("storeId") Long storeId, @Param("date") LocalDate date);

    @Query("""
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.orderItems
        WHERE o.userId = :userId
        ORDER BY o.orderAt DESC
        """)
    List<Order> findAllByUserIdWithAll(@Param("userId") Long userId);

    @Query("""
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.orderItems
        WHERE o.userId = :userId
          AND o.status <> com.babpul.orderservice.order.OrderStatus.COMPLETED
        ORDER BY o.pickupAt ASC
        """)
    List<Order> findActiveOrdersByUserIdWithAll(@Param("userId") Long userId);
}