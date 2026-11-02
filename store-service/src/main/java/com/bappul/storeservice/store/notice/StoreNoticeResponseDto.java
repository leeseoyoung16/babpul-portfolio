package com.bappul.storeservice.store.notice;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class StoreNoticeResponseDto {

    private Long id;
    private Long storeId;
    private String title;
    private String content;
    private LocalDateTime scheduledAt;
    private LocalDateTime expireAt;
    private boolean active;
    private LocalDateTime createdAt;

    public StoreNoticeResponseDto(StoreNotice notice) {
        this.id = notice.getId();
        this.storeId = notice.getStore().getId();
        this.title = notice.getTitle();
        this.content = notice.getContent();
        this.scheduledAt = notice.getScheduledAt();
        this.expireAt = notice.getExpireAt();
        this.active = notice.isActive();
        this.createdAt = notice.getCreatedAt();
    }
}
