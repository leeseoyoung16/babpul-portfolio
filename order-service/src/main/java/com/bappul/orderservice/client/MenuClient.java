package com.bappul.orderservice.client;

import com.bappul.orderservice.client.dto.MenuDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "store-service", contextId = "menuClient")
public interface MenuClient {

    @GetMapping("/internal/menus/store/{storeId}")
    List<MenuDto> getMenusByStore(@PathVariable Long storeId);
}