package com.babpul.orderservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtStompAuthenticator {

    private final JwtProvider jwtProvider;

    public Authentication getAuthentication(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;

        String token = authHeader.substring(7);
        if (!jwtProvider.validateToken(token)) return null;

        String uid = jwtProvider.getUidFromToken(token);
        Long userId = jwtProvider.getUserIdFromToken(token);
        String role = jwtProvider.getRoleFromToken(token);

        AuthenticatedUser authUser = new AuthenticatedUser(userId, uid, role);
        return new UsernamePasswordAuthenticationToken(
                authUser, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}