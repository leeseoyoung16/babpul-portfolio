package com.bappul.userservice.auth;

import com.bappul.userservice.user.User;
import com.bappul.userservice.user.UserGender;
import com.bappul.userservice.user.UserMajor;
import com.bappul.userservice.user.UserRepository;
import com.bappul.userservice.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        final String uid;
        final String name;

        switch (provider) {
            case "kakao" -> {
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                uid = String.valueOf(attributes.get("id"));
                name = (String) profile.get("nickname");
            }
            case "google" -> {
                uid = (String) attributes.get("sub");
                name = (String) attributes.get("name");
            }
            case "naver" -> {
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                uid = (String) response.get("id");
                name = (String) response.get("name");
            }
            default -> throw new IllegalArgumentException("지원하지 않는 provider: " + provider);
        }

        User user = userRepository.findByUidAndProvider(uid, provider)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUid(uid);
                    newUser.setProvider(provider);
                    newUser.setUsername(name);
                    newUser.setMajor(UserMajor.컴퓨터공학과);
                    newUser.setGender(UserGender.FEMALE);
                    newUser.setRole(UserRole.USER);
                    return userRepository.save(newUser);
                });

        return new CustomOAuth2User(user, attributes);
    }
}