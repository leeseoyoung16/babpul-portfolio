package com.bappul.orderservice.orderitem;

import com.bappul.orderservice.order.Order;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // store-service Menu는 JPA 관계 없이 ID만 저장
    @Column(name = "menu_id_snap", nullable = false)
    private Long menuId;

    @Column(name = "menu_name", nullable = false)
    private String menuName;

    @Column(name = "price_snap", nullable = false)
    private Integer priceSnap;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "line_amount", nullable = false)
    private Integer lineAmount;

    @Column(name = "real_name")
    private String realName;

    @Column(name = "real_price")
    private Integer realPrice;

    @Column(name = "random_price")
    private Integer randomPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}