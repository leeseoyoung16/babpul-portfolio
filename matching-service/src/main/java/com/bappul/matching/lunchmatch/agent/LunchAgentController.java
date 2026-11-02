package com.bappul.matching.lunchmatch.agent;

import com.bappul.matching.config.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lunch/agent")
@RequiredArgsConstructor
public class LunchAgentController {

    private final LunchAgentService lunchAgentService;

    @PostMapping
    public AgentResponseDto chat(
            @RequestBody AgentRequestDto request,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        String reply = lunchAgentService.chat(request.message(), authUser.getUserId());
        return new AgentResponseDto(reply);
    }

    @DeleteMapping("/history")
    public AgentResponseDto clearHistory(@AuthenticationPrincipal AuthenticatedUser authUser) {
        lunchAgentService.clearHistory(authUser.getUserId());
        return new AgentResponseDto("대화 이력이 초기화되었습니다.");
    }
}