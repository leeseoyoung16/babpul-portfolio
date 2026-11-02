package com.babpul.userservice.user;

import com.babpul.userservice.notification.FcmRequestDto;
import com.babpul.userservice.notification.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 다른 MSA 서비스에서 내부적으로 호출하는 전용 API
 * Security 인증 없이 접근 가능 (내부 네트워크 전용)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class InternalUserController {

    private final UserService userService;
    private final FcmService fcmService;

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @GetMapping("/users/{userId}/fcm-token")
    public ResponseEntity<String> getFcmToken(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getFcmToken(userId));
    }

    @PostMapping("/fcm/send")
    public ResponseEntity<Void> sendFcm(@RequestBody FcmRequestDto dto) {
        fcmService.sendOrderStatusNotification(dto.getToken(), dto.getTitle(), dto.getBody());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}/points")
    public ResponseEntity<Void> updatePoints(@PathVariable Long userId, @RequestParam int delta) {
        userService.updatePoints(userId, delta);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<Void> updateRole(@PathVariable Long userId, @RequestParam String role) {
        userService.updateRole(userId, role);
        return ResponseEntity.ok().build();
    }
}