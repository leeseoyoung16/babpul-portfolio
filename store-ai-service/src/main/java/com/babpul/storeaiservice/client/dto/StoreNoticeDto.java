package com.babpul.storeaiservice.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class StoreNoticeDto {
    private Long id;
    private Long storeId;
    private String title;
    private String content;
    private LocalDateTime scheduledAt;
    private LocalDateTime expireAt;
    private boolean active;
    private LocalDateTime createdAt;
}