package com.bappul.storeservice.store.notice;

import com.bappul.storeservice.config.AuthenticatedUser;
import com.bappul.storeservice.store.Store;
import com.bappul.storeservice.store.StoreRepository;
import com.bappul.storeservice.store.StoreSocketPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notices")
public class StoreNoticeController {

    private final StoreNoticeService noticeService;
    private final StoreRepository storeRepository;
    private final StoreSocketPublisher storeSocketPublisher;

    /** 공지 등록 POST /notices/my */
    @PostMapping("/my")
    public ResponseEntity<StoreNoticeResponseDto> createNotice(
            @Valid @RequestBody StoreNoticeRequestDto requestDto,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        Store store = storeRepository.findByOwnerId(authUser.getUserId())
                .orElseThrow(() -> new RuntimeException("운영 중인 가게가 없습니다."));

        StoreNotice notice = noticeService.createNotice(
                store.getId(),
                requestDto.getTitle(),
                requestDto.getContent(),
                requestDto.getScheduledAt(),
                requestDto.getExpireAt(),
                authUser.getUserId()
        );
        storeSocketPublisher.sendNoticeCreated(store);

        return ResponseEntity.ok(new StoreNoticeResponseDto(notice));
    }

    /** 공지 수정 PUT /notices/{noticeId} */
    @PutMapping("/{noticeId}")
    public StoreNoticeResponseDto updateNotice(
            @PathVariable Long noticeId,
            @Valid @RequestBody StoreNoticeRequestDto requestDto,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        StoreNotice updated = noticeService.updateNotice(
                noticeId,
                requestDto.getTitle(),
                requestDto.getContent(),
                requestDto.getScheduledAt(),
                requestDto.getExpireAt(),
                authUser.getUserId()
        );
        storeSocketPublisher.sendNoticeUpdated(updated.getStore());

        return new StoreNoticeResponseDto(updated);
    }

    /** 특정 상점 공지 조회 GET /notices/stores/{storeId} */
    @GetMapping("/stores/{storeId}")
    public ResponseEntity<List<StoreNoticeResponseDto>> getNoticesByStore(@PathVariable Long storeId) {
        List<StoreNoticeResponseDto> responseList = noticeService.getNoticesByStore(storeId)
                .stream()
                .map(StoreNoticeResponseDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    /** 전체 활성 공지 조회 GET /notices/active */
    @GetMapping("/active")
    public ResponseEntity<List<StoreNoticeResponseDto>> getAllActiveNotices() {
        List<StoreNoticeResponseDto> responseList = noticeService.getAllActiveNotices()
                .stream()
                .map(StoreNoticeResponseDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    /** 공지 삭제 DELETE /notices/{noticeId} */
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<String> deleteNotice(
            @PathVariable Long noticeId,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        Store store = noticeService.deleteNoticeAndReturnStore(noticeId, authUser.getUserId());
        storeSocketPublisher.sendNoticeDeleted(store);

        return ResponseEntity.ok("공지사항이 삭제되었습니다.");
    }
}
