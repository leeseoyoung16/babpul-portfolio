package com.babpul.storeaiservice.client;

import com.babpul.storeaiservice.client.dto.StoreNoticeDto;
import com.babpul.storeaiservice.client.dto.StoreScheduleDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "store-service", contextId = "storeServiceClient", fallback = StoreServiceClientFallback.class)
public interface StoreServiceClient {

    // ── 공지 ──
    @GetMapping("/internal/stores/{storeId}/notices")
    List<StoreNoticeDto> getNotices(@PathVariable Long storeId);

    @PostMapping("/internal/stores/{storeId}/notices")
    StoreNoticeDto createNotice(@PathVariable Long storeId,
                                @RequestBody Map<String, String> body,
                                @RequestParam Long ownerId);

    @PutMapping("/internal/stores/notices/{noticeId}")
    StoreNoticeDto updateNotice(@PathVariable Long noticeId,
                                @RequestBody Map<String, String> body,
                                @RequestParam Long ownerId);

    @DeleteMapping("/internal/stores/notices/{noticeId}")
    void deleteNotice(@PathVariable Long noticeId, @RequestParam Long ownerId);

    // ── 스케줄 ──
    @GetMapping("/internal/stores/{storeId}/schedules")
    List<StoreScheduleDto> getSchedules(@PathVariable Long storeId);

    @PutMapping("/internal/stores/{storeId}/schedules/{dayOfWeek}")
    List<StoreScheduleDto> updateSchedule(@PathVariable Long storeId,
                                           @PathVariable String dayOfWeek,
                                           @RequestBody Map<String, Object> body,
                                           @RequestParam Long ownerId);

    // ── 영업 상태 ──
    @GetMapping("/internal/stores/{storeId}/open")
    Map<String, Boolean> isStoreOpen(@PathVariable Long storeId);

    // ── 상점 조회 (ownerId 기반) ──
    @GetMapping("/internal/stores/owner/{ownerId}")
    Map<String, Object> getStoreByOwnerId(@PathVariable Long ownerId);
}