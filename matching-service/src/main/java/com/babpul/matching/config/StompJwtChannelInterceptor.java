package com.babpul.matching.config;

import com.babpul.matching.chat.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    // 와일드카드 구독(/sub/chat/room/*)을 막기 위해 숫자 ID만 허용
    private static final Pattern CHAT_ROOM_DESTINATION = Pattern.compile("^/sub/chat/room/(\\d{1,18})$");

    private final JwtProvider jwtProvider;
    // ChatService는 SimpMessagingTemplate에 의존해 순환 참조가 생기므로 Repository를 직접 사용
    private final ChatRoomRepository chatRoomRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        switch (accessor.getCommand()) {
            case CONNECT, STOMP -> authenticate(accessor);
            case SEND -> authorizeSend(accessor);
            case SUBSCRIBE -> authorizeSubscribe(accessor);
            default -> { }
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String auth = accessor.getFirstNativeHeader("Authorization");
        if (!StringUtils.hasText(auth) || !auth.startsWith("Bearer ")) {
            log.warn("[WS AUTH] Missing or malformed Authorization header");
            throw new BadCredentialsException("인증이 필요합니다.");
        }

        String token = auth.substring(7);
        if (!jwtProvider.validateToken(token)) {
            log.warn("[WS AUTH] Invalid token");
            throw new BadCredentialsException("인증이 필요합니다.");
        }

        String uid    = jwtProvider.getUidFromToken(token);
        Long   userId = jwtProvider.getUserIdFromToken(token);
        String role   = jwtProvider.getRoleFromToken(token);

        AuthenticatedUser authUser = new AuthenticatedUser(userId, uid, role);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        authUser, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));

        accessor.setUser(authentication);
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            sessionAttributes.put("WS_AUTH_PRINCIPAL", authentication);
        }
        log.info("[WS AUTH] STOMP user authenticated: userId={}", userId);
    }

    private void authorizeSend(StompHeaderAccessor accessor) {
        requireUser(accessor);

        String destination = accessor.getDestination();
        // /sub/** 직접 SEND는 브로커가 그대로 전달하므로 /pub/** 외에는 거부
        if (destination == null || !destination.startsWith("/pub/")) {
            log.warn("[WS AUTH] SEND denied: destination={}", destination);
            throw new AccessDeniedException("허용되지 않은 목적지입니다.");
        }
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        AuthenticatedUser user = requireUser(accessor);

        String destination = accessor.getDestination();
        Matcher matcher = destination != null ? CHAT_ROOM_DESTINATION.matcher(destination) : null;
        if (matcher == null || !matcher.matches() || !isParticipant(Long.valueOf(matcher.group(1)), user.getUserId())) {
            log.warn("[WS AUTH] SUBSCRIBE denied: userId={}, destination={}", user.getUserId(), destination);
            throw new AccessDeniedException("구독 권한이 없습니다.");
        }
    }

    private boolean isParticipant(Long roomId, Long userId) {
        return chatRoomRepository.findById(roomId)
                .map(room -> room.getUser1Id().equals(userId) || room.getUser2Id().equals(userId))
                .orElse(false);
    }

    private AuthenticatedUser requireUser(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof AuthenticatedUser authUser) {
            return authUser;
        }
        throw new BadCredentialsException("인증이 필요합니다.");
    }
}
