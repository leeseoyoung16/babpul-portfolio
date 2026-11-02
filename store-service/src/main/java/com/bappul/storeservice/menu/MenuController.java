package com.bappul.storeservice.menu;

import com.bappul.storeservice.config.AuthenticatedUser;
import com.bappul.storeservice.store.StoreRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/menus")
public class MenuController {

    private final MenuService menuService;
    private final StoreRepository storeRepository;

    @PostMapping("/store")
    public ResponseEntity<MenuResponseDto> createMenu(
            @Valid @ModelAttribute MenuRequestDto dto,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        Long storeId = storeRepository.findByOwnerId(authUser.getUserId())
                .orElseThrow(() -> new RuntimeException("운영 중인 매장을 찾을 수 없습니다.")).getId();
        Menu menu = menuService.createMenu(storeId, authUser.getUserId(),
                dto.getName(), dto.getPrice(), dto.getPrepTime(), dto.getImage());
        return ResponseEntity.ok(new MenuResponseDto(menu));
    }

    @PutMapping("/{menuId}")
    public ResponseEntity<MenuResponseDto> updateMenu(
            @PathVariable Long menuId,
            @Valid @ModelAttribute MenuRequestDto dto,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        Menu menu = menuService.updateMenu(menuId, authUser.getUserId(),
                dto.getName(), dto.getPrice(), dto.getPrepTime(), dto.getImage());
        return ResponseEntity.ok(new MenuResponseDto(menu));
    }

    @DeleteMapping("/{menuId}")
    public ResponseEntity<String> disableMenu(
            @PathVariable Long menuId,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        menuService.disableMenu(menuId, authUser.getUserId());
        return ResponseEntity.ok("메뉴가 비활성화되었습니다.");
    }

    @PatchMapping("/{menuId}/soldout")
    public ResponseEntity<MenuResponseDto> toggleSoldOut(
            @PathVariable Long menuId,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.ok(new MenuResponseDto(menuService.toggleSoldOut(menuId, authUser.getUserId())));
    }

    @GetMapping("/stores/{storeId}")
    public ResponseEntity<List<MenuResponseDto>> getMenusByStore(@PathVariable Long storeId) {
        return ResponseEntity.ok(menuService.getMenusByStore(storeId).stream().map(MenuResponseDto::new).toList());
    }

    @GetMapping("/{menuId}")
    public ResponseEntity<MenuResponseDto> getMenu(@PathVariable Long menuId) {
        return ResponseEntity.ok(new MenuResponseDto(menuService.getMenu(menuId)));
    }

    @PatchMapping("/{menuId}/weight")
    public ResponseEntity<MenuResponseDto> updateWeight(
            @PathVariable Long menuId,
            @RequestParam int weight,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.ok(new MenuResponseDto(menuService.updateWeight(menuId, weight, authUser.getRole())));
    }
}