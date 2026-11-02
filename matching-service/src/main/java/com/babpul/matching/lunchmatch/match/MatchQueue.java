package com.babpul.matching.lunchmatch.match;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.time.DayOfWeek;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@RedisHash("MatchQueue")
public class MatchQueue {

    @Id
    private String id;

    private Long userId;

    private DayOfWeek dayOfWeek;

    private List<FreeTimeSlot> requestedSlots;

    private MatchQueueStatus status;

    private String proposalId;

    @TimeToLive(unit = TimeUnit.SECONDS)
    private long ttl = 300;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FreeTimeSlot {
        private String startTime;
        private String endTime;
    }
}