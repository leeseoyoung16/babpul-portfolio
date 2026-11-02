package com.babpul.storeaiservice.aiagent;

import com.babpul.storeaiservice.client.StoreServiceClient;
import com.babpul.storeaiservice.config.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final StoreServiceClient storeServiceClient;

    @PostMapping
    public AgentResponseDto chat(
            @RequestBody AgentRequestDto request,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        Map<String, Object> storeInfo = storeServiceClient.getStoreByOwnerId(authUser.getUserId());
        if (storeInfo == null) {
            return new AgentResponseDto("등록된 가게가 없습니다. 먼저 가게를 등록해 주세요.");
        }
        Long storeId = Long.valueOf(storeInfo.get("id").toString());
        String reply = agentService.chat(storeId, request.message(), authUser.getUserId());
        return new AgentResponseDto(reply);
    }

    @DeleteMapping("/history")
    public AgentResponseDto clearHistory(@AuthenticationPrincipal AuthenticatedUser authUser) {
        agentService.clearHistory(authUser.getUserId());
        return new AgentResponseDto("대화 이력이 초기화되었습니다.");
    }
}