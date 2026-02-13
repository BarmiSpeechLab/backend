package com.example.backend.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    // 상수정의
    USER("ROLE_USER", "일반 사용자"),
    TUTOR("ROLE_TUTOR","튜터");

    // 필드 정의
    private final String key;   // 시큐리티에서 사용
    private final String title; // UI에서 보여줄 설명
}
