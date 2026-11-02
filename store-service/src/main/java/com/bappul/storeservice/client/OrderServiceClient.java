package com.bappul.storeservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/internal/orders/store/{storeId}/has-active")
    Boolean hasActiveOrders(@PathVariable Long storeId);
}