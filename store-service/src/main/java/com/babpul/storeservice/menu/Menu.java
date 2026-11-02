package com.babpul.storeservice.menu;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "menu")
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    private String name;

    private int price;

    @Column(name = "is_soldout")
    private boolean isSoldout;

    @Column(name = "prep_time")
    private int prepTime;

    @Column(name = "img_url")
    private String imgUrl;

    @Column(name = "is_available")
    private boolean isAvailable;

    @Column(nullable = false)
    private int weight = 1;
}