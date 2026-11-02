package com.babpul.userservice.user;

import lombok.Getter;

@Getter
public class UserResponseDto {
    private Long id;
    private String username;
    private String email;
    private String provider;
    private String role;
    private String major;
    private String gender;
    private Integer grade;
    private String fcmToken;
    private int points;

    public UserResponseDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.provider = user.getProvider();
        this.role = (user.getRole() != null) ? user.getRole().name() : null;
        this.major = (user.getMajor() != null) ? user.getMajor().name() : null;
        this.gender = (user.getGender() != null) ? user.getGender().name() : null;
        this.grade = user.getGrade();
        this.fcmToken = user.getFcmToken();
        this.points = user.getPoints();
    }
}