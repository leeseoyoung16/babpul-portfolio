package com.bappul.orderservice.client;

import com.bappul.orderservice.client.dto.MenuDto;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * store-service가 아직 분리되지 않은 경우 사용하는 fallback
 * store-service 구현 후 제거 예정
 */
@Component
public class MenuClientFallback implements MenuClient {

    @Override
    public List<MenuDto> getMenusByStore(Long storeId) {
        throw new RuntimeException("store-service 연결 불가 - store-service가 아직 실행 중이지 않습니다.");
    }
}
