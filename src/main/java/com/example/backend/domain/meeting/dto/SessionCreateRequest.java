package com.example.backend.domain.meeting.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionCreateRequest {
    // 사용자가 원하는 방 ID (NULL이면 랜덤 생성)
    private String customSessionId;
}
