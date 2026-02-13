package com.example.backend.domain.meeting.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MeetingJoinResponse {
    private String sessionId;
    private String token;
}
