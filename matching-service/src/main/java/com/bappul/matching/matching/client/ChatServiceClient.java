package com.bappul.matching.matching.client;

import com.bappul.matching.matching.dto.ChatRoomResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * chat-service 연동 Feign Client.
 * TODO (3주차): chat-service 분리 완료 후 실제 엔드포인트 확정 필요
 */
@FeignClient(name = "chat-service")
public interface ChatServiceClient {

    @PostMapping("/api/chat/rooms/get-or-create")
    ChatRoomResponse getOrCreateChatRoom(
            @RequestParam("userAId") Long userAId,
            @RequestParam("userBId") Long userBId
    );
}
