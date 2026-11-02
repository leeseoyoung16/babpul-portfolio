package com.babpul.orderservice.client;

import com.babpul.orderservice.client.dto.MenuDto;
import com.babpul.orderservice.client.dto.StoreInfoDto;
import org.springframework.stereotype.Component;

/**
 * store-service가 아직 분리되지 않은 경우 사용하는 fallback
 * store-service 구현 후 제거 예정
 */
@Component
public class StoreServiceFallback implements StoreServiceClient {

    @Override
    public MenuDto getMenu(Long menuId) {
        throw new RuntimeException("store-service 연결 불가 - store-service가 아직 실행 중이지 않습니다.");
    }

    @Override
    public StoreInfoDto getStoreInfo(Long storeId) {
        throw new RuntimeException("store-service 연결 불가 - store-service가 아직 실행 중이지 않습니다.");
    }

    @Override
    public StoreInfoDto getStoreByOwnerId(Long ownerId) {
        throw new RuntimeException("store-service 연결 불가 - store-service가 아직 실행 중이지 않습니다.");
    }
}