package com.babpul.matching.lunchmatch.match;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.util.concurrent.TimeUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@RedisHash("MatchProposal")
public class MatchProposal {

    @Id
    private String id;

    private Long requesterId;
    private Long receiverId;

    private String matchedDate;
    private String matchedStartTime;
    private String matchedEndTime;

    private boolean requesterAccepted;
    private boolean receiverAccepted;

    private MatchProposalStatus status;

    @Builder.Default
    @TimeToLive(unit = TimeUnit.SECONDS)
    private long ttl = 300;
}