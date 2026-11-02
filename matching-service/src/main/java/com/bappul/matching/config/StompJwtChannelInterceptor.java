package com.bappul.matching.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String auth = null;
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

            if (sessionAttributes != null) {
                Object fromSession = sessionAttributes.get("passcode");
                if (fromSession instanceof String s) auth = s;
            }
            if (!StringUtils.hasText(auth)) auth = accessor.getFirstNativeHeader("Authorization");
            if (!StringUtils.hasText(auth)) auth = accessor.getFirstNativeHeader("passcode");

            if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
                String token = auth.substring(7);
                if (jwtProvider.validateToken(token)) {
                    String uid    = jwtProvider.getUidFromToken(token);
                    Long   userId = jwtProvider.getUserIdFromToken(token);
                    String role   = jwtProvider.getRoleFromToken(token);

                    AuthenticatedUser authUser = new AuthenticatedUser(userId, uid, role);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    authUser, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));

                    accessor.setUser(authentication);
                    if (sessionAttributes != null) {
                        sessionAttributes.put("WS_AUTH_PRINCIPAL", authentication);
                    }
                    log.info("[WS AUTH] STOMP user authenticated: userId={}", userId);
                }
            } else {
                log.warn("[WS AUTH] Missing or malformed Authorization header");
            }
        }

        return message;
    }
}