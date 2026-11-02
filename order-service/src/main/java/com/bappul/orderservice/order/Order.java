package com.bappul.orderservice.order;

import com.bappul.orderservice.orderitem.OrderItem;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 다른 서비스 엔티티는 JPA 관계 없이 ID만 저장 (MSA 원칙)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "store_name_snapshot")
    private String storeNameSnapshot;

    @Column(name = "owner_id_snapshot")
    private Long ownerIdSnapshot;

    @Column(name = "user_name_snapshot")
    private String userNameSnapshot;

    @Column(name = "total_price")
    private Integer totalPrice;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    private OrderMode mode;

    @Column(name = "order_at")
    private LocalDateTime orderAt;

    @Column(name = "pickup_at")
    private LocalDateTime pickupAt;

    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    private String requirement;

    @Column(name = "is_random", nullable = false)
    private boolean random = false;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "daily_seq")
    private Integer dailySeq;

    @Column(name = "order_number")
    private String orderNumber;


    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;
}