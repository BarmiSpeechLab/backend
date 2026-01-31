package com.example.backend.domain.user.dto;

import com.example.backend.domain.user.entity.Role;
import com.example.backend.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.lang.reflect.Member;

@Getter
@Builder
public class UserResponse {
    private String email;
    private String nickname;
    private String profileImgUrl;
    private Role role;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImgUrl(user.getProfileImgUrl()) // ★ Getter 이름 확인
                .role(user.getRole())
                .build();
    }
}
