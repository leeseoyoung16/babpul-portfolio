package com.babpul.storeaiservice.client;

import com.babpul.storeaiservice.client.dto.MenuDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "store-service", contextId = "menuClient", fallback = MenuClientFallback.class)
public interface MenuClient {

    @GetMapping("/internal/menus/store/{storeId}")
    List<MenuDto> getMenusByStore(@PathVariable Long storeId);

    @GetMapping("/internal/menus/{menuId}")
    MenuDto getMenu(@PathVariable Long menuId);

    @PostMapping("/internal/menus/store/{storeId}")
    MenuDto createMenu(@PathVariable Long storeId,
                       @RequestBody Map<String, Object> body,
                       @RequestParam Long ownerId);

    @PutMapping("/internal/menus/{menuId}")
    MenuDto updateMenu(@PathVariable Long menuId,
                       @RequestBody Map<String, Object> body,
                       @RequestParam Long ownerId);

    @PatchMapping("/internal/menus/{menuId}/disable")
    void disableMenu(@PathVariable Long menuId, @RequestParam Long ownerId);

    @PatchMapping("/internal/menus/{menuId}/soldout")
    MenuDto setSoldOut(@PathVariable Long menuId, @RequestParam boolean soldOut, @RequestParam Long ownerId);
}