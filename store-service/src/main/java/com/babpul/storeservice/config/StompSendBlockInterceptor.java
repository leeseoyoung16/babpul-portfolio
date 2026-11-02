package com.babpul.storeservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** store-service의 소켓은 서버 발행 전용이므로, 위조를 막기 위해 클라이언트 SEND를 거부한다. */
@Slf4j
@Component
public class StompSendBlockInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.SEND.equals(accessor.getCommand())) {
            log.warn("[WS] SEND denied: destination={}", accessor.getDestination());
            throw new AccessDeniedException("허용되지 않은 요청입니다.");
        }

        return message;
    }
}
