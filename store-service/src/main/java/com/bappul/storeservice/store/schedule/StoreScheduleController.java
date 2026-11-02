package com.bappul.storeservice.store.schedule;

import com.bappul.storeservice.config.AuthenticatedUser;
import com.bappul.storeservice.store.Store;
import com.bappul.storeservice.store.StoreRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/schedules")
public class StoreScheduleController {

    private final StoreScheduleService scheduleService;
    private final StoreRepository storeRepository;

    /** 스케줄 등록/수정 PUT /schedules/my */
    @PutMapping("/my")
    public ResponseEntity<String> createOrUpdateScheduleForMyStore(
            @Valid @RequestBody List<StoreScheduleRequestDto> scheduleDtos,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        Store store = storeRepository.findByOwnerId(authUser.getUserId())
                .orElseThrow(() -> new RuntimeException("운영 중인 가게가 없습니다."));

        List<StoreSchedule> schedules = scheduleDtos.stream()
                .map(StoreScheduleRequestDto::toEntity)
                .collect(Collectors.toList());

        scheduleService.createOrUpdateSchedule(store.getId(), schedules, authUser.getUserId());

        return ResponseEntity.ok("가게 영업 스케줄이 등록(수정)되었습니다.");
    }

    /** 특정 가게의 스케줄 전체 조회 GET /schedules/stores/{storeId} */
    @GetMapping("/stores/{storeId}")
    public ResponseEntity<List<StoreScheduleResponseDto>> getSchedulesByStore(@PathVariable Long storeId) {
        List<StoreScheduleResponseDto> responseList = scheduleService.getSchedulesByStore(storeId)
                .stream()
                .map(StoreScheduleResponseDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    /** 현재 영업 중인지 확인 GET /schedules/stores/{storeId}/open */
    @GetMapping("/stores/{storeId}/open")
    public ResponseEntity<String> isStoreOpen(@PathVariable Long storeId) {
        boolean isOpen = scheduleService.isStoreOpen(storeId);
        return ResponseEntity.ok(isOpen ? "영업 중입니다." : "영업 종료되었습니다.");
    }
}
