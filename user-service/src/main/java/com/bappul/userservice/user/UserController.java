package com.bappul.userservice.user;

import com.bappul.userservice.auth.JwtProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/users")
public class UserController {

    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponseDto> completeSignup(
            @Valid @RequestBody SignupRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        User user = userDetails.getUser();
        User updated = userService.completeSignup(requestDto, user);

        String newAccessToken = jwtProvider.generateToken(
                updated.getUid(), updated.getProvider(), updated.getRole().name(), updated.getId());
        String newRefreshToken = jwtProvider.generateRefreshToken(updated.getUid());

        updated.setRefreshToken(newRefreshToken);
        userRepository.save(updated);

        return ResponseEntity.ok(new SignupResponseDto(
                new UserResponseDto(updated), newAccessToken, newRefreshToken));
    }

    @PatchMapping("/profile")
    public ResponseEntity<UserResponseDto> updateUserProfile(
            @Valid @RequestBody UpdateRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        User updated = userService.updateUserProfile(requestDto, userDetails.getUser());
        return ResponseEntity.ok(new UserResponseDto(updated));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getUser(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(new UserResponseDto(userDetails.getUser()));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("인증이 필요합니다.");
        }
        userService.logout(userDetails.getUser());
        return ResponseEntity.ok("로그아웃 완료");
    }

    @GetMapping("/majors")
    public UserMajor[] getAllMajors() {
        return UserMajor.values();
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<Void> registerFcmToken(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String fcmToken = body.get("fcmToken");
        if (fcmToken == null || fcmToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        userService.updateFcmToken(userDetails.getUser(), fcmToken);
        return ResponseEntity.ok().build();
    }
}