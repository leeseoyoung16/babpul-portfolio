package com.babpul.userservice.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getUserByUidAndProvider(String uid, String provider) {
        return userRepository.findByUidAndProvider(uid, provider)
                .orElseThrow(() -> new RuntimeException("해당 사용자를 찾지 못했습니다."));
    }

    @Transactional(readOnly = true)
    public User getUserEntityById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("해당 사용자를 찾을 수 없습니다. (ID: " + userId + ")"));
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return new UserResponseDto(user);
    }

    @Transactional(readOnly = true)
    public String getFcmToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return user.getFcmToken();
    }

    @Transactional
    public User completeSignup(SignupRequestDto requestDto, User user) {
        User found = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        found.setUsername(requestDto.getUsername());
        found.setEmail(requestDto.getEmail());
        found.setGender(requestDto.getGender());
        found.setMajor(requestDto.getMajor());
        found.setGrade(requestDto.getGrade());
        return userRepository.save(found);
    }

    @Transactional
    public User updateUserProfile(UpdateRequestDto requestDto, User user) {
        User found = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        if (requestDto.getUsername() != null) found.setUsername(requestDto.getUsername());
        if (requestDto.getEmail() != null) found.setEmail(requestDto.getEmail());
        if (requestDto.getGender() != null) found.setGender(requestDto.getGender());
        if (requestDto.getMajor() != null) found.setMajor(requestDto.getMajor());
        if (requestDto.getGrade() != null) found.setGrade(requestDto.getGrade());
        return userRepository.save(found);
    }

    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void logout(User user) {
        user.setRefreshToken(null);
        userRepository.save(user);
    }

    @Transactional
    public void updateFcmToken(User user, String fcmToken) {
        User found = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        found.setFcmToken(fcmToken);
        userRepository.save(found);
    }

    @Transactional
    public void updateRole(Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        user.setRole(UserRole.valueOf(role));
        userRepository.save(user);
    }

    @Transactional
    public void updatePoints(Long userId, int delta) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        int newPoints = user.getPoints() + delta;
        if (newPoints < 0) throw new IllegalStateException("포인트가 부족합니다.");
        user.setPoints(newPoints);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Long findInternalIdByExternalId(String externalId) {
        return userRepository.findByUid(externalId)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }
}