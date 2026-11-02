package com.bappul.orderservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private static final String WS_AUTH_PRINCIPAL = "WS_AUTH_PRINCIPAL";

    private final JwtStompAuthenticator authenticator;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        Authentication authentication = null;

        // 1. 세션 속성에서 토큰 확인
        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
        if (sessionAttrs != null) {
            String passcode = (String) sessionAttrs.get("passcode");
            if (passcode != null) {
                authentication = authenticator.getAuthentication("Bearer " + passcode);
            }
        }

        // 2. Authorization 헤더에서 확인
        if (authentication == null) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null) {
                authentication = authenticator.getAuthentication(authHeader);
            }
        }

        // 3. passcode 헤더에서 확인 (SockJS fallback)
        if (authentication == null) {
            String passcode = accessor.getPasscode();
            if (passcode != null) {
                authentication = authenticator.getAuthentication("Bearer " + passcode);
            }
        }

        if (authentication != null) {
            accessor.setUser(authentication);
            if (sessionAttrs != null) {
                sessionAttrs.put(WS_AUTH_PRINCIPAL, authentication);
            }
        }

        return message;
    }
}