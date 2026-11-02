package com.bappul.userservice.config;

import com.bappul.userservice.user.User;
import com.bappul.userservice.user.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Set<String> ALLOWED_REDIRECT_URIS = Set.of("myapp://login");

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        String accessToken = jwtProvider.generateToken(
                user.getUid(), user.getProvider(), user.getRole().name(), user.getId());
        String refreshToken = jwtProvider.generateRefreshToken(user.getUid());

        user.setRefreshToken(refreshToken);
        userRepository.save(user);
        userRepository.flush();

        String redirectUri = request.getParameter("redirect_uri");
        if (redirectUri == null || redirectUri.isBlank() || !ALLOWED_REDIRECT_URIS.contains(redirectUri)) {
            redirectUri = "myapp://login";
        }

        String encodedAccess = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        String encodedRefresh = URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

        String targetUrl = redirectUri
                + "?accessToken=" + encodedAccess
                + "&refreshToken=" + encodedRefresh;

        log.info(">>> OAuth2 로그인 성공: " + targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}