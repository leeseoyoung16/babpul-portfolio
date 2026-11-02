package com.babpul.orderservice.cart;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CartRepository extends JpaRepository<Cart, Long> {

    boolean existsByUserIdAndStatus(Long userId, CartStatus status);

    @Query("""
        SELECT c FROM Cart c
        LEFT JOIN FETCH c.items
        WHERE c.userId = :userId AND c.status = :status
        """)
    List<Cart> findAllByUserIdAndStatusWithFullItems(@Param("userId") Long userId,
                                                     @Param("status") CartStatus status);
}