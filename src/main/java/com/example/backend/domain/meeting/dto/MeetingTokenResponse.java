package com.example.backend.domain.meeting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MeetingTokenResponse {
    private String token; // OpenVidu가 발급해준 접속 토큰
}
