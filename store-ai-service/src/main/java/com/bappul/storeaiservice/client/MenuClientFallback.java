package com.bappul.storeaiservice.client;

import com.bappul.storeaiservice.client.dto.MenuDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MenuClientFallback implements MenuClient {

    @Override
    public List<MenuDto> getMenusByStore(Long storeId) {
        throw new RuntimeException("store-service 연결 불가 - 메뉴 목록을 조회할 수 없습니다.");
    }

    @Override
    public MenuDto getMenu(Long menuId) {
        throw new RuntimeException("store-service 연결 불가 - 메뉴를 조회할 수 없습니다.");
    }

    @Override
    public MenuDto createMenu(Long storeId, Map<String, Object> body, Long ownerId) {
        throw new RuntimeException("store-service 연결 불가 - 메뉴를 등록할 수 없습니다.");
    }

    @Override
    public MenuDto updateMenu(Long menuId, Map<String, Object> body, Long ownerId) {
        throw new RuntimeException("store-service 연결 불가 - 메뉴를 수정할 수 없습니다.");
    }

    @Override
    public void disableMenu(Long menuId, Long ownerId) {
        throw new RuntimeException("store-service 연결 불가 - 메뉴를 비활성화할 수 없습니다.");
    }

    @Override
    public MenuDto toggleSoldOut(Long menuId, Long ownerId) {
        throw new RuntimeException("store-service 연결 불가 - 품절 상태를 변경할 수 없습니다.");
    }
}
