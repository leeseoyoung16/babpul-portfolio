package com.bappul.orderservice.randomorder;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "random_order_config")
public class RandomOrderConfig {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private int price;
}