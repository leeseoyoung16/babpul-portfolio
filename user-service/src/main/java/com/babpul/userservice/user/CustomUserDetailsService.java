package com.babpul.userservice.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService {

    private final UserRepository userRepository;

    public UserDetails loadUserByUidAndProvider(String uid, String provider) {
        User user = userRepository.findByUidAndProvider(uid, provider)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + uid + ", provider: " + provider));
        return new CustomUserDetails(user);
    }
}