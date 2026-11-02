package com.babpul.orderservice.client;

import com.babpul.orderservice.client.dto.MenuDto;
import com.babpul.orderservice.client.dto.StoreInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "store-service", contextId = "storeServiceClient", fallback = StoreServiceFallback.class)
public interface StoreServiceClient {

    @GetMapping("/internal/menus/{menuId}")
    MenuDto getMenu(@PathVariable Long menuId);

    @GetMapping("/internal/stores/{storeId}")
    StoreInfoDto getStoreInfo(@PathVariable Long storeId);

    @GetMapping("/internal/stores/owner/{ownerId}")
    StoreInfoDto getStoreByOwnerId(@PathVariable Long ownerId);
}