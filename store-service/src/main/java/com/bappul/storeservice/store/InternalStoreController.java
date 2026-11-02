package com.bappul.storeservice.store;

import com.bappul.storeservice.store.notice.StoreNotice;
import com.bappul.storeservice.store.notice.StoreNoticeResponseDto;
import com.bappul.storeservice.store.notice.StoreNoticeService;
import com.bappul.storeservice.store.schedule.StoreSchedule;
import com.bappul.storeservice.store.schedule.StoreScheduleResponseDto;
import com.bappul.storeservice.store.schedule.StoreScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/stores")
public class InternalStoreController {

    private final StoreNoticeService noticeService;
    private final StoreScheduleService scheduleService;
    private final StoreRepository storeRepository;

    @GetMapping("/{storeId}")
    public ResponseEntity<StoreResponseDto> getStoreInfo(@PathVariable Long storeId) {
        Store store = storeRepository.findById(storeId).orElse(null);
        if (store == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new StoreResponseDto(store));
    }

    // ── 공지 ────────────────────────────────────────

    @GetMapping("/{storeId}/notices")
    public ResponseEntity<List<StoreNoticeResponseDto>> getNotices(@PathVariable Long storeId) {
        List<StoreNoticeResponseDto> list = noticeService.getNoticesByStore(storeId)
                .stream().map(StoreNoticeResponseDto::new).toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{storeId}/notices")
    public ResponseEntity<StoreNoticeResponseDto> createNotice(
            @PathVariable Long storeId,
            @RequestBody Map<String, String> body,
            @RequestParam Long ownerId
    ) {
        StoreNotice notice = noticeService.createNotice(
                storeId,
                body.get("title"),
                body.get("content"),
                LocalDateTime.parse(body.get("scheduledAt")),
                LocalDateTime.parse(body.get("expireAt")),
                ownerId
        );
        return ResponseEntity.ok(new StoreNoticeResponseDto(notice));
    }

    @PutMapping("/notices/{noticeId}")
    public ResponseEntity<StoreNoticeResponseDto> updateNotice(
            @PathVariable Long noticeId,
            @RequestBody Map<String, String> body,
            @RequestParam Long ownerId
    ) {
        StoreNotice notice = noticeService.updateNotice(
                noticeId,
                body.get("title"),
                body.get("content"),
                LocalDateTime.parse(body.get("scheduledAt")),
                LocalDateTime.parse(body.get("expireAt")),
                ownerId
        );
        return ResponseEntity.ok(new StoreNoticeResponseDto(notice));
    }

    @DeleteMapping("/notices/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long noticeId, @RequestParam Long ownerId) {
        noticeService.deleteNoticeAndReturnStore(noticeId, ownerId);
        return ResponseEntity.ok().build();
    }

    // ── 스케줄 ────────────────────────────────────────

    @GetMapping("/{storeId}/schedules")
    public ResponseEntity<List<StoreScheduleResponseDto>> getSchedules(@PathVariable Long storeId) {
        List<StoreScheduleResponseDto> list = scheduleService.getSchedulesByStore(storeId)
                .stream().map(StoreScheduleResponseDto::new).toList();
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{storeId}/schedules/{dayOfWeek}")
    public ResponseEntity<List<StoreScheduleResponseDto>> updateSchedule(
            @PathVariable Long storeId,
            @PathVariable String dayOfWeek,
            @RequestBody Map<String, Object> body,
            @RequestParam Long ownerId
    ) {
        boolean closed = Boolean.TRUE.equals(body.get("closed"));
        String openTimeStr  = (String) body.getOrDefault("openTime",  "00:00");
        String closeTimeStr = (String) body.getOrDefault("closeTime", "00:00");

        StoreSchedule updated = new StoreSchedule();
        updated.setDayOfWeek(DayOfWeek.valueOf(dayOfWeek.toUpperCase()));
        updated.setClosed(closed);
        updated.setOpenTime(closed ? LocalTime.of(0, 0) : LocalTime.parse(openTimeStr));
        updated.setCloseTime(closed ? LocalTime.of(0, 0) : LocalTime.parse(closeTimeStr));

        List<StoreSchedule> existing = scheduleService.getSchedulesByStore(storeId);
        List<StoreSchedule> result = new ArrayList<>(existing.stream()
                .map(s -> s.getDayOfWeek() == updated.getDayOfWeek() ? updated : s)
                .toList());
        boolean found = existing.stream().anyMatch(s -> s.getDayOfWeek() == updated.getDayOfWeek());
        if (!found) result.add(updated);

        scheduleService.createOrUpdateSchedule(storeId, result, ownerId);

        return ResponseEntity.ok(scheduleService.getSchedulesByStore(storeId)
                .stream().map(StoreScheduleResponseDto::new).toList());
    }

    @GetMapping("/{storeId}/open")
    public ResponseEntity<Map<String, Boolean>> isStoreOpen(@PathVariable Long storeId) {
        return ResponseEntity.ok(Map.of("open", scheduleService.isStoreOpen(storeId)));
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<StoreResponseDto> getStoreByOwnerId(@PathVariable Long ownerId) {
        Store store = storeRepository.findByOwnerId(ownerId).orElse(null);
        if (store == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new StoreResponseDto(store));
    }
}