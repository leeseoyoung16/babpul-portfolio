package com.babpul.storeservice.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByStoreIdAndIsAvailableTrue(Long storeId);
    List<Menu> findByStoreIdAndIsAvailableTrueAndIsSoldoutFalse(Long storeId);
}