package com.babpul.orderservice.cartitem;

import com.babpul.orderservice.cart.Cart;
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
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    // store-service Menu는 JPA 관계 없이 ID + 스냅샷으로 저장
    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    private String menuName;
    private Integer unitPrice;
    private Integer quantity;
    private Integer lineAmount;
    private Integer prepTime;

    public void calculateLineAmount() {
        this.lineAmount = this.unitPrice * this.quantity;
    }
}
