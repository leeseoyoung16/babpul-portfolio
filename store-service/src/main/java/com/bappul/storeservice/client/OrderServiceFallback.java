package com.bappul.storeservice.client;

import org.springframework.stereotype.Component;

@Component
public class OrderServiceFallback implements OrderServiceClient {

    @Override
    public Boolean hasActiveOrders(Long storeId) {
        throw new RuntimeException("order-service 연결 불가 - 진행 중인 주문 여부를 확인할 수 없습니다.");
    }
}
