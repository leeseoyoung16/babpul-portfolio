package com.babpul.matching.point;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PointHistoryResponseDto {
    private final Long id;
    private final PointType type;
    private final int amount;
    private final String reason;
    private final LocalDateTime createdAt;

    public PointHistoryResponseDto(PointHistory h) {
        this.id        = h.getId();
        this.type      = h.getType();
        this.amount    = h.getAmount();
        this.reason    = h.getReason();
        this.createdAt = h.getCreatedAt();
    }
}