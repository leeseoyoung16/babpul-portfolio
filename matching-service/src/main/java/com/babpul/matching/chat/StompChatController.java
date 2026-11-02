package com.babpul.matching.chat;

import com.babpul.matching.config.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class StompChatController {

    private final ChatService chatService;

    @MessageMapping("/message")
    public void sendMessage(
            @Payload @Valid ChatMessageRequestDto messageDto,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Principal principal = headerAccessor.getUser();

        if (principal == null) {
            Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
            if (sessionAttributes != null) {
                principal = (Principal) sessionAttributes.get("WS_AUTH_PRINCIPAL");
            }
        }

        if (principal == null) {
            log.warn("[STOMP] 인증 실패");
            return;
        }

        Long senderId = extractUserId(principal);
        if (senderId == null) {
            log.warn("[STOMP] userId 추출 실패");
            return;
        }

        chatService.saveAndPublishMessage(senderId, messageDto.getRoomId(), messageDto.getContent(), messageDto.getType());
    }

    private Long extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof AuthenticatedUser authUser) {
            return authUser.getUserId();
        }
        return null;
    }
}