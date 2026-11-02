package com.bappul.matching.matching;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchingCleanupScheduler {

    private final MatchingService matchingService;

    @Scheduled(fixedRate = 300000)
    public void runCleanupJob() {
        System.out.println("[CLEANUP] Checking for expired matching attempts...");
        matchingService.cleanupExpiredAttempts();
    }

    @Scheduled(fixedRate = 60000)
    public void runScheduledMatchJob() {
        matchingService.findAndMatchPendingUsers();
    }
}
