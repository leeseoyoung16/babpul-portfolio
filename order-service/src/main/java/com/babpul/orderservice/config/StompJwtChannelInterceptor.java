package com.babpul.orderservice.config;

import com.babpul.orderservice.client.StoreServiceClient;
import com.babpul.orderservice.client.dto.StoreInfoDto;
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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private static final String WS_AUTH_PRINCIPAL = "WS_AUTH_PRINCIPAL";

    // 와일드카드 구독(/sub/user/*)을 막기 위해 숫자 ID만 허용
    private static final Pattern USER_DESTINATION = Pattern.compile("^/sub/user/(\\d{1,18})$");
    private static final Pattern STORE_DESTINATION = Pattern.compile("^/sub/store/(\\d{1,18})$");

    private final JwtStompAuthenticator authenticator;
    private final StoreServiceClient storeServiceClient;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

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
        Authentication authentication = authenticator.getAuthentication(accessor.getFirstNativeHeader("Authorization"));
        if (authentication == null) {
            log.warn("[WS AUTH] Missing or invalid Authorization header");
            throw new BadCredentialsException("인증이 필요합니다.");
        }

        accessor.setUser(authentication);
        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
        if (sessionAttrs != null) {
            sessionAttrs.put(WS_AUTH_PRINCIPAL, authentication);
        }
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
        if (destination == null || !canSubscribe(destination, user.getUserId())) {
            log.warn("[WS AUTH] SUBSCRIBE denied: userId={}, destination={}", user.getUserId(), destination);
            throw new AccessDeniedException("구독 권한이 없습니다.");
        }
    }

    private boolean canSubscribe(String destination, Long userId) {
        Matcher userMatcher = USER_DESTINATION.matcher(destination);
        if (userMatcher.matches()) {
            return Long.valueOf(userMatcher.group(1)).equals(userId);
        }

        Matcher storeMatcher = STORE_DESTINATION.matcher(destination);
        if (storeMatcher.matches()) {
            // store-service 장애 시 fallback이 예외를 던지므로 구독은 거부된다
            StoreInfoDto store = storeServiceClient.getStoreInfo(Long.valueOf(storeMatcher.group(1)));
            return store != null && store.getOwnerId() != null && store.getOwnerId().equals(userId);
        }

        return false;
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
