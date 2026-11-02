package com.babpul.storeservice.menu;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/menus")
public class InternalMenuController {

    private final MenuService menuService;

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<MenuResponseDto>> getMenusByStore(@PathVariable Long storeId) {
        return ResponseEntity.ok(menuService.getMenusByStore(storeId).stream().map(MenuResponseDto::new).toList());
    }

    @GetMapping("/store/{storeId}/orderable")
    public ResponseEntity<List<MenuResponseDto>> getOrderableMenus(@PathVariable Long storeId) {
        return ResponseEntity.ok(menuService.getOrderableMenus(storeId).stream().map(MenuResponseDto::new).toList());
    }

    @GetMapping("/{menuId}")
    public ResponseEntity<MenuResponseDto> getMenu(@PathVariable Long menuId) {
        return ResponseEntity.ok(new MenuResponseDto(menuService.getMenu(menuId)));
    }

    @PostMapping("/store/{storeId}")
    public ResponseEntity<MenuResponseDto> createMenu(
            @PathVariable Long storeId,
            @RequestBody Map<String, Object> body,
            @RequestParam Long ownerId
    ) {
        Menu menu = menuService.createMenu(
                storeId, ownerId,
                (String) body.get("name"),
                (Integer) body.get("price"),
                (Integer) body.get("prepTime"),
                null
        );
        return ResponseEntity.ok(new MenuResponseDto(menu));
    }

    @PutMapping("/{menuId}")
    public ResponseEntity<MenuResponseDto> updateMenu(
            @PathVariable Long menuId,
            @RequestBody Map<String, Object> body,
            @RequestParam Long ownerId
    ) {
        Menu menu = menuService.updateMenu(
                menuId, ownerId,
                (String) body.get("name"),
                (Integer) body.get("price"),
                (Integer) body.get("prepTime"),
                null
        );
        return ResponseEntity.ok(new MenuResponseDto(menu));
    }

    @PatchMapping("/{menuId}/disable")
    public ResponseEntity<Void> disableMenu(@PathVariable Long menuId, @RequestParam Long ownerId) {
        menuService.disableMenu(menuId, ownerId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{menuId}/soldout")
    public ResponseEntity<MenuResponseDto> toggleSoldOut(@PathVariable Long menuId, @RequestParam Long ownerId) {
        return ResponseEntity.ok(new MenuResponseDto(menuService.toggleSoldOut(menuId, ownerId)));
    }
}