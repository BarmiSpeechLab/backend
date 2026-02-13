package com.example.backend.domain.meeting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SessionResponse {
    private String sessionId; // 프론트에서는 "data.sessionId"로 접근
}
