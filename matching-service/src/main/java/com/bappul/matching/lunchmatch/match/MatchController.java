package com.bappul.matching.lunchmatch.match;

import com.bappul.matching.client.UserServiceClient;
import com.bappul.matching.client.dto.UserInfoDto;
import com.bappul.matching.config.AuthenticatedUser;
import com.bappul.matching.lunchmatch.timetable.FreeTimeResponseDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/lunch/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;
    private final MatchSseService matchSseService;
    private final UserServiceClient userServiceClient;

    @GetMapping("/free-times")
    public List<FreeTimeResponseDto> getTodayFreeTimes(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return matchService.getTodayFreeTimes(authUser.getUserId())
                .stream().map(FreeTimeResponseDto::new).toList();
    }

    @PostMapping("/enqueue")
    public MatchEnqueueResult enqueue(
            @RequestBody List<MatchQueue.FreeTimeSlot> requestedSlots,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        UserInfoDto userInfo = userServiceClient.getUserById(authUser.getUserId());
        return matchService.enqueue(authUser.getUserId(), userInfo.getPoints(), requestedSlots);
    }

    @GetMapping("/status")
    public MatchQueue getStatus(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return matchService.getQueueStatus(authUser.getUserId());
    }

    @GetMapping("/proposal/{proposalId}")
    public MatchProposal getProposal(@PathVariable String proposalId) {
        return matchService.getProposal(proposalId);
    }

    @PostMapping("/proposal/{proposalId}/respond")
    public MatchProposalResult respond(
            @PathVariable String proposalId,
            @RequestParam boolean accept,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return matchService.respond(authUser.getUserId(), proposalId, accept);
    }

    @DeleteMapping("/cancel")
    public void cancel(@AuthenticationPrincipal AuthenticatedUser authUser) {
        matchService.cancelQueue(authUser.getUserId());
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal AuthenticatedUser authUser,
                                HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        return matchSseService.subscribe(authUser.getUserId());
    }
}