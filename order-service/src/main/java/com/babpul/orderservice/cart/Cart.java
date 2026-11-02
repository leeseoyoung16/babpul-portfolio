package com.babpul.orderservice.cart;

import com.babpul.orderservice.cartitem.CartItem;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // user-service User는 JPA 관계 없이 ID만 저장
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    private CartStatus status = CartStatus.ACTIVE;

    private Integer totalPrice = 0;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.status = CartStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void markCheckedOut() {
        this.status = CartStatus.CHECKED_OUT;
    }

    public void recalculateTotal() {
        this.totalPrice = items.stream()
                .mapToInt(CartItem::getLineAmount)
                .sum();
    }

    public void addItem(CartItem item) {
        item.setCart(this);
        items.add(item);
        recalculateTotal();
    }
}