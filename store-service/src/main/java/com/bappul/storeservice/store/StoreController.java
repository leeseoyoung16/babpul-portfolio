package com.bappul.storeservice.store;

import com.bappul.storeservice.client.UserResponseDto;
import com.bappul.storeservice.client.UserServiceClient;
import com.bappul.storeservice.config.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stores")
public class StoreController {

    private final StoreService storeService;
    private final UserServiceClient userServiceClient;

    /** 상점 등록 POST /stores */
    @PostMapping
    public ResponseEntity<StoreResponseDto> createStore(
            @Valid @RequestBody StoreRequestDto requestDto,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        UserResponseDto userInfo = userServiceClient.getUserById(authUser.getUserId());

        Store store = storeService.createStore(
                requestDto.getName(),
                requestDto.getAddress(),
                requestDto.getPhone(),
                authUser.getUserId(),
                userInfo.getUsername()
        );

        return ResponseEntity.ok(new StoreResponseDto(store));
    }

    /** 상점 정보 수정 PATCH /stores/my */
    @PatchMapping("/my")
    public ResponseEntity<StoreResponseDto> updateMyStore(
            @Valid @RequestBody StoreRequestDto requestDto,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        Store store = storeService.getActiveStoreByOwnerId(authUser.getUserId());

        Store updated = storeService.updateStore(
                store.getId(),
                requestDto.getName(),
                requestDto.getAddress(),
                requestDto.getPhone(),
                authUser.getUserId()
        );

        return ResponseEntity.ok(new StoreResponseDto(updated));
    }

    /** 상점 폐업 처리 PATCH /stores/my/close */
    @PatchMapping("/my/close")
    public ResponseEntity<String> closeMyStore(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        Store store = storeService.getActiveStoreByOwnerId(authUser.getUserId());
        storeService.closeStore(store.getId(), authUser.getUserId());

        return ResponseEntity.ok("상점이 폐업 처리되었습니다. (isAvailable=false)");
    }

    /** 폐업하지 않은 상점 단일 조회 GET /stores/{storeId} */
    @GetMapping("/{storeId}")
    public ResponseEntity<StoreResponseDto> getStore(@PathVariable Long storeId) {
        Store store = storeService.getStore(storeId);
        return ResponseEntity.ok(new StoreResponseDto(store));
    }

    /** 사장님 본인 상점 조회 GET /stores/my */
    @GetMapping("/my")
    public ResponseEntity<StoreResponseDto> getMyStore(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        Store store = storeService.getActiveStoreByOwnerId(authUser.getUserId());
        return ResponseEntity.ok(new StoreResponseDto(store));
    }

    /** 영업 상태 수동 변경 PATCH /stores/my/open?isOpen=true|false */
    @PatchMapping("/my/open")
    public ResponseEntity<String> updateMyStoreOpenStatus(
            @RequestParam boolean isOpen,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        Store store = storeService.getActiveStoreByOwnerId(authUser.getUserId());
        storeService.updateStoreOpenStatus(store.getId(), isOpen, authUser.getUserId());

        return ResponseEntity.ok(
                isOpen ? "상점이 영업을 시작했습니다." : "상점이 수동으로 영업을 종료했습니다."
        );
    }

    /** 폐업하지 않은 모든 상점 조회 GET /stores */
    @GetMapping
    public ResponseEntity<List<StoreResponseDto>> getAllActiveStores() {
        List<StoreResponseDto> responseList = storeService.getAllActiveStores()
                .stream()
                .map(StoreResponseDto::new)
                .toList();

        return ResponseEntity.ok(responseList);
    }
}
